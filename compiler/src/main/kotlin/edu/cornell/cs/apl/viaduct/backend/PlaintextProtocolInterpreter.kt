package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

class PlaintextProtocolInterpreter(
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val runtime: ViaductProcessRuntime
) : AbstractProtocolInterpreter<PlaintextClassObject>(program) {
    private val tempStoreStack: Stack<PersistentMap<Temporary, Value>> = Stack()

    private var tempStore: PersistentMap<Temporary, Value>
        get() {
            return tempStoreStack.peek()
        }
        set(value) {
            tempStoreStack.pop()
            tempStoreStack.push(value)
        }

    init {
        assert(runtime.projection.protocol is Local || runtime.projection.protocol is Replication)

        objectStoreStack.push(persistentMapOf())
        tempStoreStack.push(persistentMapOf())
    }

    private fun pushContext(
        newObjectStore: PersistentMap<ObjectVariable, ObjectLocation>,
        newTempStore: PersistentMap<Temporary, Value>
    ) {
        objectStoreStack.push(newObjectStore)
        tempStoreStack.push(newTempStore)
    }

    override suspend fun pushContext() {
        pushContext(this.objectStore, this.tempStore)
    }

    override suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf())
    }

    override suspend fun popContext() {
        objectStoreStack.pop()
        tempStoreStack.pop()
    }

    override fun getContextMarker(): Int {
        return objectStoreStack.size
    }

    override fun restoreContext(marker: Int) {
        while (objectStoreStack.size > marker) {
            objectStoreStack.pop()
            tempStoreStack.pop()
        }
    }

    override fun allocateObject(obj: PlaintextClassObject): ObjectLocation {
        objectHeap.add(obj)
        return objectHeap.size - 1
    }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): PlaintextClassObject {
        return ImmutableCellObject(runExpr(expr))
    }

    override suspend fun buildObject(
        className: ClassNameNode,
        typeArguments: Arguments<ValueTypeNode>,
        arguments: Arguments<AtomicExpressionNode>
    ): PlaintextClassObject {
        return when (className.value) {
            ImmutableCell -> ImmutableCellObject(runExpr(arguments[0]))

            MutableCell -> MutableCellObject(runExpr(arguments[0]))

            Vector -> {
                val length = runExpr(arguments[0]) as IntegerValue
                VectorObject(length.value, length.type.defaultValue)
            }

            else -> throw Exception("runtime error")
        }
    }

    override fun getNullObject(): PlaintextClassObject = NullObject

    private suspend fun runRead(read: ReadNode): Value {
        val storeValue = tempStore[read.temporary.value]
        return if (storeValue == null) {
            val sendProtocol = protocolAnalysis.primaryProtocol(read)

            // must receive from read protocol
            if (runtime.projection.protocol != sendProtocol) {
                val sendPhase: ProtocolCommunication = SimpleProtocolComposer.communicate(sendProtocol, runtime.projection.protocol)

                var finalValue: Value? = null
                for (recvEvent: CommunicationEvent in sendPhase.getHostReceives(runtime.projection.host)) {
                    val receivedValue: Value =
                        runtime.receive(ProtocolProjection(sendProtocol, recvEvent.send.host))

                    if (finalValue == null) {
                        finalValue = receivedValue
                    } else if (finalValue != receivedValue) {
                        throw ViaductInterpreterError("received different values")
                    }
                }

                tempStore = tempStore.put(read.temporary.value, finalValue!!)
                finalValue
            } else { // temporary should be stored locally, but isn't
                throw UndefinedNameError(read.temporary)
            }
        } else {
            storeValue
        }
    }

    suspend fun runExpr(expr: ExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value

            is ReadNode -> runRead(expr)

            is OperatorApplicationNode -> {
                val argValues = expr.arguments.map { runExpr(it) }
                expr.operator.apply(argValues)
            }

            is QueryNode -> {
                objectStore[expr.variable.value]?.let { loc ->
                    val argValues: List<Value> = expr.arguments.map { arg -> runExpr(arg) }
                    objectHeap[loc].query(expr.query, argValues)
                } ?: throw UndefinedNameError(expr.variable)
            }

            is DowngradeNode -> runExpr(expr.expression)

            is InputNode -> {
                when (runtime.projection.protocol) {
                    is Local -> runtime.input()

                    else -> throw ViaductInterpreterError("Cannot perform I/O in non-Local protocol", expr)
                }
            }

            is ReceiveNode -> TODO()
        }
    }

    override suspend fun runExprAsValue(expr: AtomicExpressionNode): Value = runExpr(expr)

    override suspend fun runLet(stmt: LetNode) {
        val rhsValue = runExpr(stmt.value)
        tempStore = tempStore.put(stmt.temporary.value, rhsValue)

        // broadcast to other protocols
        val recvProtocols =
            protocolAnalysis.directReaders(stmt).filter { it != runtime.projection.protocol }

        for (recvProtocol: Protocol in recvProtocols) {
            val sendPhase: ProtocolCommunication = SimpleProtocolComposer.communicate(runtime.projection.protocol, recvProtocol)

            for (sendEvent in sendPhase.getHostSends(runtime.projection.host)) {
                runtime.send(rhsValue, ProtocolProjection(recvProtocol, sendEvent.recv.host))
            }
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        val argValues: List<Value> = stmt.arguments.map { arg -> runExpr(arg) }
        getObject(getObjectLocation(stmt.variable.value)).update(stmt.update, argValues)
    }

    override suspend fun runOutput(stmt: OutputNode) {
        when (runtime.projection.protocol) {
            is Local -> {
                val outputValue = runExpr(stmt.message)
                runtime.output(outputValue)
            }

            else -> throw ViaductInterpreterError("Cannot perform I/O in non-Local protocol", stmt)
        }
    }

    companion object : ProtocolInterpreterFactory {
        override fun buildProtocolInterpreter(
            program: ProgramNode,
            protocolAnalysis: ProtocolAnalysis,
            runtime: ViaductProcessRuntime,
            connectionMap: Map<Host, HostAddress>
        ): ProtocolInterpreter {
            return PlaintextProtocolInterpreter(program, protocolAnalysis, runtime)
        }
    }
}
