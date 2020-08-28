package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.commitment.HashInfo
import edu.cornell.cs.apl.viaduct.backend.commitment.encode
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/** Backend for Local and Replication protocols. */
class PlaintextBackend(
    private val typeAnalysis: TypeAnalysis
) : ProtocolBackend {

    override suspend fun run(runtime: ViaductProcessRuntime, process: BlockNode) {
        val interpreter = PlaintextInterpreter(typeAnalysis, runtime)

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
    private val typeAnalysis: TypeAnalysis,
    private val runtime: ViaductProcessRuntime
) : AbstractBackendInterpreter() {
    private val projection: ProtocolProjection = runtime.projection

    private val objectStoreStack: Stack<PersistentMap<ObjectVariable, PlaintextClassObject>> = Stack()

    private var objectStore: PersistentMap<ObjectVariable, PlaintextClassObject>
        get() {
            return objectStoreStack.peek()
        }
        set(value) {
            objectStoreStack.pop()
            objectStoreStack.push(value)
        }

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

    override fun pushContext() {
        objectStoreStack.push(objectStore)
        tempStoreStack.push(tempStore)
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

    private suspend fun replicationCleartextReceive(node: ReceiveNode): Value {
        val sendProtocol = node.protocol.value
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

    private suspend fun receiveCommitment(protocol: Commitment): Value {
        val nonce = runtime.receive(ProtocolProjection(protocol, protocol.cleartextHost)) as ByteVecValue
        val msg = runtime.receive(ProtocolProjection(protocol, protocol.cleartextHost))

        for (hashHost: Host in protocol.hashHosts) {
            val h = runtime.receive(ProtocolProjection(protocol, hashHost)) as ByteVecValue
            assert(HashInfo(h.value, nonce.value).verify(msg.encode()))
        }
        return msg
    }

    private suspend fun runExpr(expr: ExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value

            is ReadNode -> {
                tempStore[expr.temporary.value]
                    ?: throw UndefinedNameError(expr.temporary)
            }

            is QueryNode -> {
                val argValues: List<Value> = expr.arguments.map { arg -> runExpr(arg) }

                objectStore[expr.variable.value]?.let { obj ->
                    obj.query(expr.query, argValues)
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

                            sendProtocol is Commitment -> {
                                receiveCommitment(sendProtocol)
                            }

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
                        when (expr.protocol.value) {
                            is Commitment -> receiveCommitment(expr.protocol.value)
                            else -> replicationCleartextReceive(expr)
                        }
                    }

                    else -> throw ViaductInterpreterError(
                        "cannot receive from protocol $sendProtocol from $projection"
                    )
                }
            }
        }
    }

    override suspend fun runAtomicExpr(expr: AtomicExpressionNode): Value {
        return runExpr(expr)
    }

    override suspend fun runDeclaration(stmt: DeclarationNode) {
        val objectType: ObjectType = typeAnalysis.type(stmt)
        val arguments: List<Value> = stmt.arguments.map { arg -> runExpr(arg) }

        when (objectType) {
            is ImmutableCellType -> {
                objectStore =
                    objectStore.put(
                        stmt.variable.value,
                        ImmutableCellObject(arguments[0], stmt.variable, objectType)
                    )
            }

            is MutableCellType -> {
                objectStore =
                    objectStore.put(
                        stmt.variable.value,
                        MutableCellObject(arguments[0], stmt.variable, objectType)
                    )
            }

            is VectorType -> {
                val length = arguments[0] as IntegerValue
                objectStore =
                    objectStore.put(
                        stmt.variable.value,
                        VectorObject(length.value, objectType.elementType.defaultValue, stmt.variable, objectType)
                    )
            }

            else -> throw UndefinedNameError(stmt.className)
        }
    }

    override suspend fun runLet(stmt: LetNode) {
        val rhsValue: Value = runExpr(stmt.value)
        tempStore = tempStore.put(stmt.temporary.value, rhsValue)
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        val argValues: List<Value> = stmt.arguments.map { arg -> runExpr(arg) }

        objectStore[stmt.variable.value]
            ?.update(stmt.update, argValues)
            ?: throw UndefinedNameError(stmt.variable)
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
