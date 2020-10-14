package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.backend.commitment.HashInfo
import edu.cornell.cs.apl.viaduct.backend.commitment.encode
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
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
import edu.cornell.cs.apl.viaduct.syntax.types.UnitType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import java.util.Stack

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
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): PlaintextClassObject =
        when (className) {
            ImmutableCell -> ImmutableCellObject(runExpr(arguments[0]))

            MutableCell -> MutableCellObject(runExpr(arguments[0]))

            Vector -> {
                val length = runExpr(arguments[0]) as IntegerValue
                VectorObject(length.value, length.type.defaultValue)
            }

            else -> throw Exception("runtime error")
        }

    override fun getNullObject(): PlaintextClassObject = NullObject

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

                if (sendProtocol is Commitment) { // TODO: integrate in with protocol ports
                    if (expr.type.value is UnitType) { // Ignore syncs for now with commitment
                        return UnitValue
                    }
                    else {
                        return receiveCommitment(sendProtocol)
                    }
                }

                when (expr.type.value) {
                    is UnitType -> {
                        val syncPhase = SimpleProtocolComposer.getSyncPhase(sendProtocol, projection.protocol)
                        for (recvEvent: CommunicationEvent in syncPhase.getHostReceives(this.projection.host)) {
                            runtime.receive(ProtocolProjection(sendProtocol, recvEvent.send.host))
                        }

                        UnitValue
                    }

                    else -> {
                        val sendPhase = SimpleProtocolComposer.getSendPhase(sendProtocol, projection.protocol)

                        var finalValue: Value? = null
                        for (recvEvent: CommunicationEvent in sendPhase.getHostReceives(this.projection.host)) {
                            val receivedValue: Value =
                                runtime.receive(ProtocolProjection(sendProtocol, recvEvent.send.host))

                            if (finalValue == null) {
                                finalValue = receivedValue
                            } else if (finalValue != receivedValue) {
                                throw ViaductInterpreterError("received different values")
                            }
                        }

                        val broadcastPhase = SimpleProtocolComposer.getBroadcastPhase(sendProtocol, projection.protocol)
                        if (finalValue != null) {
                            for (sendEvent: CommunicationEvent in broadcastPhase.getHostSends(this.projection.host)) {
                                runtime.send(
                                    finalValue,
                                    ProtocolProjection(sendEvent.recv.protocol, sendEvent.recv.host)
                                )
                            }
                        } else {
                            for (recvEvent: CommunicationEvent in broadcastPhase.getHostReceives(this.projection.host)) {
                                val receivedValue: Value =
                                    runtime.receive(ProtocolProjection(sendProtocol, recvEvent.send.host))

                                if (finalValue == null) {
                                    finalValue = receivedValue
                                } else if (finalValue != receivedValue) {
                                    throw ViaductInterpreterError("received different values")
                                }
                            }
                        }

                        finalValue!!
                    }
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

        if (recvProtocol is Commitment) { // TODO: merge this in with ports idea
            if (!(msgValue is UnitValue)) { // Ignore syncs for now
                runtime.send(msgValue, ProtocolProjection(recvProtocol, recvProtocol.cleartextHost))
            }
        } else {
            val phase =
                when (msgValue) {
                    UnitValue -> SimpleProtocolComposer.getSyncPhase(this.projection.protocol, recvProtocol)
                    else -> SimpleProtocolComposer.getSendPhase(this.projection.protocol, recvProtocol)
                }

            for (sendEvent in phase.getHostSends(this.projection.host)) {
                runtime.send(msgValue, ProtocolProjection(recvProtocol, sendEvent.recv.host))
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
