package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.prettyprinting.joined
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.commitment.HashInfo
import edu.cornell.cs.apl.viaduct.backend.commitment.encode
import edu.cornell.cs.apl.viaduct.backends.cleartext.Local
import edu.cornell.cs.apl.viaduct.backends.cleartext.Plaintext
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import mu.KotlinLogging
import java.util.Stack

private val logger = KotlinLogging.logger("Plaintext")

class PlaintextProtocolInterpreter(
    program: ProgramNode,
    protocols: Set<Protocol>,
    private val host: Host,
    private val runtime: ViaductRuntime
) : AbstractProtocolInterpreter<PlaintextClassObject>(program) {
    override val availableProtocols: Set<Protocol> = protocols

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

    override suspend fun buildExpressionObject(protocol: Protocol, expr: AtomicExpressionNode): PlaintextClassObject {
        return ImmutableCellObject(runExpr(expr))
    }

    override suspend fun buildObject(
        protocol: Protocol,
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): PlaintextClassObject {
        return when (className) {
            ImmutableCell -> ImmutableCellObject(runExpr(arguments[0]))

            MutableCell -> MutableCellObject(runExpr(arguments[0]))

            Vector -> {
                val length = runExpr(arguments[0]) as IntegerValue
                VectorObject(length.value, length.type.defaultValue)
            }

            else -> throw Exception("runtime error")
        }
    }

    override fun getNullObject(protocol: Protocol): PlaintextClassObject = NullObject

    private fun runRead(read: ReadNode): Value =
        tempStore[read.temporary.value]
            ?: throw ViaductInterpreterError("Plaintext: could not find local temporary ${read.temporary.value}")

    private suspend fun runExpr(expr: ExpressionNode): Value {
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

            is InputNode -> runtime.input()

            is ReceiveNode -> TODO()
        }
    }

    override suspend fun runGuard(protocol: Protocol, expr: AtomicExpressionNode): Value = runExpr(expr)

    override suspend fun runLet(protocol: Protocol, stmt: LetNode) {
        val rhsValue = runExpr(stmt.value)
        tempStore = tempStore.put(stmt.temporary.value, rhsValue)
    }

    override suspend fun runUpdate(protocol: Protocol, stmt: UpdateNode) {
        val argValues: List<Value> = stmt.arguments.map { arg -> runExpr(arg) }
        getObject(getObjectLocation(stmt.variable.value)).update(stmt.update, argValues)
    }

    override suspend fun runOutput(protocol: Protocol, stmt: OutputNode) {
        when (protocol) {
            is Local -> {
                val outputValue = runExpr(stmt.message)
                runtime.output(outputValue)
            }

            else -> throw ViaductInterpreterError("Cannot perform I/O in non-Local protocol", stmt)
        }
    }

    override suspend fun runSend(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ) {
        if (sendProtocol != recvProtocol) {
            val relevantEvents: Set<CommunicationEvent> =
                events.getProjectionSends(ProtocolProjection(sendProtocol, this.host))

            val rhsValue = tempStore[sender.temporary.value]!!
            for (event in relevantEvents) {
                runtime.send(rhsValue, event)
            }
        }
    }

    override suspend fun runReceive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ) {
        // must receive from read protocol
        if (sendProtocol != recvProtocol) {
            val projection = ProtocolProjection(recvProtocol, this.host)
            val cleartextInputs =
                events.getProjectionReceives(projection, Plaintext.INPUT)

            val cleartextCommitmentInputs =
                events.getProjectionReceives(projection, Plaintext.CLEARTEXT_COMMITMENT_INPUT)

            val hashCommitmentInputs =
                events.getProjectionReceives(projection, Plaintext.HASH_COMMITMENT_INPUT)

            when {
                // cleartext input
                cleartextInputs.isNotEmpty() && cleartextCommitmentInputs.isEmpty() && hashCommitmentInputs.isEmpty() -> {
                    var cleartextValue: Value? = null
                    for (event in cleartextInputs) {
                        val receivedValue: Value = runtime.receive(event)

                        if (cleartextValue == null) {
                            cleartextValue = receivedValue
                        } else if (cleartextValue != receivedValue) {
                            throw ViaductInterpreterError("Plaintext: received different values")
                        }
                    }

                    if (cleartextValue == null) {
                        throw ViaductInterpreterError("Plaintext: received null value")
                    }

                    // calculate set of hosts with whom [this.host] needs to check for equivocation
                    val hostsToCheckWith: Set<Host> =
                        events
                            .filter { event ->
                                // remove events where receiving host is not receiving plaintext data
                                event.recv.id == Plaintext.INPUT &&

                                    // remove events where a host is sending data to themselves
                                    event.send.host != event.recv.host &&

                                    // remove events where [this.host] is the sender of the data
                                    event.send.host != this.host
                            }
                            // of events matching above criteria, get set of data receivers
                            .map { event -> event.recv.host }
                            // remove [this.host] from the set of hosts with whom [this.host] needs to
                            // check for equivocation
                            .filter { host -> host != this.host }
                            .toSet()

                    if (hostsToCheckWith.isNotEmpty()) {
                        logger.trace {
                            "host: " + this.host.asDocument.print() + " checks for equivocation with: " +
                                hostsToCheckWith.sorted().joined().print()
                        }
                    }

                    for (host in hostsToCheckWith) {
                        runtime.send(
                            cleartextValue,
                            projection,
                            ProtocolProjection(recvProtocol, host)
                        )
                    }

                    for (host in hostsToCheckWith) {
                        val recvValue =
                            runtime.receive(
                                ProtocolProjection(recvProtocol, host),
                                projection
                            )

                        if (recvValue != cleartextValue) {
                            throw ViaductInterpreterError(
                                "equivocation error between hosts: " + this.host.asDocument.print() + ", " +
                                    host.asDocument.print() + ", expected " + cleartextValue.asDocument.print() +
                                    ", received " + recvValue.asDocument.print()
                            )
                        }
                    }

                    tempStore = tempStore.put(sender.temporary.value, cleartextValue)
                }

                // commitment opening
                cleartextInputs.isEmpty() && cleartextCommitmentInputs.isNotEmpty() && hashCommitmentInputs.isNotEmpty() -> {
                    assert(cleartextCommitmentInputs.size == 1)
                    val cleartextSendEvent = cleartextCommitmentInputs.first()
                    val nonce = runtime.receive(cleartextSendEvent) as ByteVecValue
                    val msg = runtime.receive(cleartextSendEvent)

                    logger.info {
                        "received opened commitment value and nonce from ${cleartextSendEvent.send.asProjection().asDocument.print()}"
                    }

                    for (hashCommitmentInput in hashCommitmentInputs) {
                        val commitment: ByteVecValue = runtime.receive(hashCommitmentInput) as ByteVecValue

                        assert(HashInfo(commitment.value, nonce.value).verify(msg.encode()))
                        logger.info {
                            "verified commitment from host ${hashCommitmentInput.send.asProjection().asDocument.print()}"
                        }
                    }

                    tempStore = tempStore.put(sender.temporary.value, msg)
                }

                else ->
                    throw ViaductInterpreterError("Plaintext: received both commitment opening and cleartext value")
            }
        }
    }

    companion object : ProtocolBackend {
        override fun buildProtocolInterpreters(
            host: Host,
            program: ProgramNode,
            protocols: Set<Protocol>,
            protocolAnalysis: ProtocolAnalysis,
            runtime: ViaductRuntime,
            connectionMap: Map<Host, HostAddress>
        ): Iterable<ProtocolInterpreter> =
            setOf(
                PlaintextProtocolInterpreter(
                    program,
                    protocols.filterIsInstance<Plaintext>().toSet(),
                    host,
                    runtime
                )
            )
    }
}
