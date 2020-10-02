package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.backend.AbstractBackendInterpreter
import edu.cornell.cs.apl.viaduct.backend.ObjectLocation
import edu.cornell.cs.apl.viaduct.backend.ProtocolProjection
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.backend.WireGenerator
import edu.cornell.cs.apl.viaduct.backend.WireTerm
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.PureExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

internal class ZKPVerifier(
    program: ProgramNode,
    private val runtime: ViaductProcessRuntime,
    private val prover: Host,
    private val verifiers: Set<Host>
) : AbstractBackendInterpreter<ZKPObject>(program) {

    private val tempStack: Stack<PersistentMap<Temporary, Value>> = Stack()
    private val projection: ProtocolProjection = runtime.projection

    private var tempStore: PersistentMap<Temporary, Value>
        get() {
            return tempStack.peek()
        }
        set(value) {
            tempStack.pop()
            tempStack.push(value)
        }

    private val wireGenerator = WireGenerator()

    private val wireStack: Stack<PersistentMap<Temporary, WireTerm>> = Stack()

    private var wireStore: PersistentMap<Temporary, WireTerm>
        get() {
            return wireStack.peek()
        }
        set(value) {
            wireStack.pop()
            wireStack.push(value)
        }

    init {
        assert(runtime.projection.protocol is ZKP)

        objectStoreStack.push(persistentMapOf())
        tempStack.push(persistentMapOf())
        wireStack.push(persistentMapOf())
    }

    override fun pushContext() {
        pushContext(objectStore)
    }

    override fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        objectStoreStack.push(initialStore)
        tempStack.push(persistentMapOf())
        wireStack.push(persistentMapOf())
    }

    override fun popContext() {
        tempStack.pop()
        objectStoreStack.pop()
        wireStack.pop()
    }

    override fun getContextMarker(): Int {
        return tempStack.size
    }

    override fun restoreContext(marker: Int) {
        while (tempStack.size > marker) {
            tempStack.pop()
            objectStoreStack.pop()
            wireStack.pop()
        }
    }

    override fun allocateObject(obj: ZKPObject): ObjectLocation {
        objectHeap.add(obj)
        return objectHeap.size - 1
    }

    private fun injectDummyIn(): WireTerm {
        return wireGenerator.mkDummyIn()
    }

    private fun injectValue(value: Value): WireTerm {
        val i = when (value) {
            is IntegerValue -> value.value
            is BooleanValue -> if (value.value) {
                1
            } else {
                0
            }
            else -> throw Exception("runtime error: unexpected value $value")
        }
        return wireGenerator.mkConst(i)
    }

    private fun getAtomicExprWire(expr: AtomicExpressionNode): WireTerm =
        when (expr) {
            is LiteralNode -> injectValue(expr.value)
            is ReadNode ->
                wireStore[expr.temporary.value] ?: throw UndefinedNameError(expr.temporary)
        }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): ZKPObject {
        return ZKPObject.ZKPImmutableCell(getAtomicExprWire(expr))
    }

    override suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): ZKPObject {
        return when (className) {
            ImmutableCell -> ZKPObject.ZKPImmutableCell(getAtomicExprWire(arguments[0]))
            MutableCell -> ZKPObject.ZKPMutableCell(getAtomicExprWire(arguments[0]))
            Vector -> {
                val length = runExprAsValue(arguments[0]) as IntegerValue
                ZKPObject.ZKPVectorObject(length.value, typeArguments[0].defaultValue)
            }
            else -> throw Exception("unknown object")
        }
    }

    override fun getNullObject(): ZKPObject {
        return ZKPObject.ZKPNullObject
    }

    override suspend fun runExprAsValue(expr: AtomicExpressionNode): Value =
        when (expr) {
        is LiteralNode -> expr.value
        is ReadNode -> tempStore[expr.temporary.value]
        ?: throw UndefinedNameError(expr.temporary)
    }

    private suspend fun runQuery(obj: ZKPObject, query: QueryNameNode, args: List<AtomicExpressionNode>): WireTerm =
        when (obj) {
            is ZKPObject.ZKPImmutableCell -> if (query.value is Get) {
                obj.value
            } else {
                throw Exception("bad query")
            }
            is ZKPObject.ZKPMutableCell -> if (query.value is Get) {
                obj.value
            } else {
                throw Exception("bad query")
            }
            is ZKPObject.ZKPVectorObject -> if (query.value is Get) {
                val index = runExprAsValue(args[0]) as IntegerValue
                obj.gates[index.value]
            } else {
                throw Exception("bad query")
            }
            ZKPObject.ZKPNullObject -> throw Exception("null query")
        }

    private suspend fun getExprWire(expr: PureExpressionNode): WireTerm =
        when (expr) {
            is LiteralNode -> getAtomicExprWire(expr)
            is ReadNode -> getAtomicExprWire(expr)
            is OperatorApplicationNode -> {
                val args = expr.arguments.map { getExprWire(it) }
                wireGenerator.mkOp(expr.operator, args)
            }
            is QueryNode -> runQuery(getObject(getObjectLocation(expr.variable.value)), expr.query, expr.arguments)
            is DeclassificationNode -> getExprWire(expr.expression)
            is EndorsementNode -> getExprWire(expr.expression)
        }

    private suspend fun replicationCleartextReceive(node: ReceiveNode): Value {
        val sendProtocol = node.protocol.value

        val sendingHosts: Set<Host> = sendProtocol.hosts.intersect(projection.protocol.hosts)
        val receivingHosts: Set<Host> = sendProtocol.hosts.minus(projection.protocol.hosts)

        if (sendingHosts.isNotEmpty()) { // at least one of the verifiers receives
            if (sendingHosts.contains(projection.host)) {
                // do actual receive
                val receivedValue: Value =
                    runtime.receive(ProtocolProjection(sendProtocol, projection.host))

                // broadcast to projections that did not receive
                for (receivingHost: Host in receivingHosts) {
                    runtime.send(receivedValue, ProtocolProjection(projection.protocol, receivingHost))
                }

                return receivedValue
            } else { // verifier does not receive; receive from copies that did
                var finalValue: Value? = null
                for (sendingHost: Host in sendingHosts) {
                    val receivedValue: Value =
                        runtime.receive(ProtocolProjection(projection.protocol, sendingHost))

                    if (finalValue == null) {
                        finalValue = receivedValue
                    } else if (finalValue != receivedValue) {
                        throw ViaductInterpreterError("received different values")
                    }
                }
                return finalValue ?: throw ViaductInterpreterError("did not receive")
            }
        } else { // no copy can receive; we cannot compile this
            throw ViaductInterpreterError("backend compilation: at least one copy of Replication process must be able to receive")
        }
    }

    override suspend fun runLet(stmt: LetNode) {
        when (val rhs: ExpressionNode = stmt.value) {
            is ReceiveNode -> {
                val receivedValue: Value = replicationCleartextReceive(rhs)
                val rhsProtocol: Protocol = rhs.protocol.value
                val isSecret = rhsProtocol.hosts.intersect(verifiers).isEmpty() // if the value was sent to any of the verifiers, the value is public
                if (isSecret) {
                    wireStore = wireStore.put(stmt.temporary.value, injectDummyIn())
                } else {
                    tempStore = tempStore.put(stmt.temporary.value, receivedValue)
                    wireStore = wireStore.put(stmt.temporary.value, injectValue(receivedValue))
                }
            }

            is InputNode -> throw Exception("cannot perform I/O in non-Local protocol")
            is PureExpressionNode ->
                wireStore = wireStore.put(stmt.temporary.value, getExprWire(rhs))
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        when (val o = getObject(getObjectLocation(stmt.variable.value))) {
            is ZKPObject.ZKPImmutableCell -> throw Exception("runtime error")
            is ZKPObject.ZKPMutableCell -> {
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        o.value = getAtomicExprWire(stmt.arguments[0])
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify -> {
                        val arg = getAtomicExprWire(stmt.arguments[0])
                        o.value = wireGenerator.mkOp(stmt.update.value.operator, listOf(o.value, arg))
                    }
                    else ->
                        throw Exception("runtime error")
                }
            }
            is ZKPObject.ZKPVectorObject -> {
                val index = runExprAsValue(stmt.arguments[0]) as IntegerValue
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        o.gates[index.value] = getAtomicExprWire(stmt.arguments[1])
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify -> {
                        val arg = getAtomicExprWire(stmt.arguments[1])
                        o.gates[index.value] =
                            wireGenerator.mkOp(stmt.update.value.operator, listOf(o.gates[index.value], arg))
                    }
                    else ->
                        throw Exception("runtime error")
                }
            }
            ZKPObject.ZKPNullObject -> throw Exception("runtime error")
        }
    }

    override suspend fun runSend(stmt: SendNode) {
        when (stmt.message) {
            is LiteralNode -> print("Sending literal ${stmt.message.value}")
            is ReadNode -> {
                // TODO: get proof from prover, verify it
                print("Being proved statement ${wireStore[stmt.message.temporary.value]}")

                runtime.receive(ProtocolProjection(runtime.projection.protocol, prover))

                for (recvHost: Host in stmt.protocol.value.hosts) {
                    runtime.send(
                        BooleanValue(true),
                        ProtocolProjection(stmt.protocol.value, recvHost)
                    )
                }
            }
        }
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw Exception("cannot perform I/O in non-Local protocol")
    }
}
