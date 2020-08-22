package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/** Backend for Local and Replication protocols. */
class PlaintextBackend : ProtocolBackend {
    override suspend fun run(
        runtime: ViaductProcessRuntime,
        program: ProgramNode,
        process: BlockNode
    ) {
        val interpreter = PlaintextInterpreter(program, runtime)

        try {
            interpreter.run(process)
        } catch (signal: LoopBreakSignal) {
            throw ViaductInterpreterError(
                "uncaught loop break signal with jump label ${signal.jumpLabel}", signal.breakNode
            )
        }
    }
}

private class PlaintextInterpreter(
    program: ProgramNode,
    private val runtime: ViaductProcessRuntime
) : AbstractBackendInterpreter<PlaintextClassObject>(program) {
    private val projection: ProtocolProjection = runtime.projection

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
        assert(projection.protocol is Local || projection.protocol is Replication)

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

    override fun pushContext() {
        pushContext(this.objectStore, this.tempStore)
    }

    override fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf())
    }

    override fun popContext() {
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
    ): PlaintextClassObject =
        when (className.value) {
            ImmutableCell -> ImmutableCellObject(runExpr(arguments[0]))

            MutableCell -> MutableCellObject(runExpr(arguments[0]))

            Vector -> {
                val length = runExpr(arguments[0]) as IntegerValue
                VectorObject(length.value, length.type.defaultValue)
            }

            else -> throw Exception("runtime error")
        }

    override fun getNullObject(): PlaintextClassObject = NullObject

    private suspend fun runExpr(expr: ExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value

            is ReadNode -> {
                tempStore[expr.temporary.value]
                    ?: throw UndefinedNameError(expr.temporary)
            }

            is QueryNode -> {
                val argValues: List<Value> = expr.arguments.map { arg -> runExpr(arg) }

                objectStore[expr.variable.value]?.let { loc ->
                    objectHeap[loc].query(expr.query, argValues)
                } ?: throw UndefinedNameError(expr.variable)
            }

            is OperatorApplicationNode -> {
                val operandValues: List<Value> = expr.arguments.map { arg -> runExpr(arg) }
                expr.operator.apply(operandValues)
            }

            is DowngradeNode -> runExpr(expr.expression)

            is InputNode -> {
                when (projection.protocol) {
                    is Local -> runtime.input()

                    else -> throw ViaductInterpreterError("Cannot perform I/O in non-Local protocol", expr)
                }
            }

            is ReceiveNode -> {
                val sendProtocol = expr.protocol.value

                when (projection.protocol) {
                    is Local -> {
                        return when {
                            // receive from local process
                            sendProtocol.hosts.contains(projection.host) -> {
                                runtime.receive(ProtocolProjection(sendProtocol, projection.host))
                            }

                            // allow remote sends from other Local processes
                            sendProtocol is Local -> {
                                runtime.receive(ProtocolProjection(sendProtocol, sendProtocol.host))
                            }

                            else -> {
                                throw ViaductInterpreterError(
                                    "cannot receive from protocol ${sendProtocol.name} to $projection"
                                )
                            }
                        }
                    }

                    is Replication -> {
                        val sendingHosts: Set<Host> = sendProtocol.hosts.intersect(projection.protocol.hosts)
                        val receivingHosts: Set<Host> = sendProtocol.hosts.minus(projection.protocol.hosts)

                        return if (!sendingHosts.isEmpty()) { // at least one copy of the Replication process receives
                            // do actual receive
                            val receivedValue: Value =
                                runtime.receive(ProtocolProjection(sendProtocol, projection.host))

                            // broadcast to projections that did not receive
                            for (receivingHost: Host in receivingHosts) {
                                runtime.send(receivedValue, ProtocolProjection(projection.protocol, receivingHost))
                            }

                            receivedValue
                        } else { // copy of Repl process at host does not receive; receive from copies that did
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

                            finalValue ?: throw ViaductInterpreterError("did not receive")
                        }
                    }

                    else -> throw ViaductInterpreterError(
                        "cannot receive from protocol $sendProtocol from $projection"
                    )
                }
            }
        }
    }

    override suspend fun runExprAsValue(expr: AtomicExpressionNode): Value {
        return runExpr(expr)
    }

    override suspend fun runLet(stmt: LetNode) {
        val rhsValue: Value = runExpr(stmt.value)
        tempStore = tempStore.put(stmt.temporary.value, rhsValue)
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        val argValues: List<Value> = stmt.arguments.map { arg -> runExpr(arg) }

        getObject(getObjectLocation(stmt.variable.value)).update(stmt.update, argValues)
    }

    override suspend fun runSend(stmt: SendNode) {
        val msgValue: Value = runExpr(stmt.message)
        val recvProtocol: Protocol = stmt.protocol.value

        when (projection.protocol) {
            is Local -> {
                if (recvProtocol.hosts.contains(projection.host)) {
                    runtime.send(msgValue, ProtocolProjection(stmt.protocol.value, projection.host))
                } else if (recvProtocol is Local) { // only send to remote Local processes
                    for (recvHost: Host in stmt.protocol.value.hosts) {
                        runtime.send(msgValue, ProtocolProjection(stmt.protocol.value, recvHost))
                    }

                    runtime.send(msgValue, ProtocolProjection(recvProtocol, recvProtocol.host))
                }
            }

            is Replication -> {
                // there is a local copy of the receiving process; send it there
                // otherwise, don't do anything
                if (recvProtocol.hosts.contains(projection.host)) {
                    runtime.send(msgValue, ProtocolProjection(recvProtocol, projection.host))
                }
            }
        }
    }

    override suspend fun runOutput(stmt: OutputNode) {
        when (projection.protocol) {
            is Local -> {
                val outputVal: Value = runExpr(stmt.message)
                runtime.output(outputVal)
            }

            else -> throw Exception("cannot perform I/O in non-Local protocol")
        }
    }
}
