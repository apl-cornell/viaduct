package io.github.aplcornell.viaduct.backend.commitment

import io.github.aplcornell.viaduct.analysis.ProtocolAnalysis
import io.github.aplcornell.viaduct.backend.ObjectLocation
import io.github.aplcornell.viaduct.backend.SingleProtocolInterpreter
import io.github.aplcornell.viaduct.backend.ViaductProcessRuntime
import io.github.aplcornell.viaduct.backends.commitment.Commitment
import io.github.aplcornell.viaduct.errors.ViaductInterpreterError
import io.github.aplcornell.viaduct.selection.CommunicationEvent
import io.github.aplcornell.viaduct.selection.ProtocolCommunication
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolProjection
import io.github.aplcornell.viaduct.syntax.QueryNameNode
import io.github.aplcornell.viaduct.syntax.Temporary
import io.github.aplcornell.viaduct.syntax.UpdateNameNode
import io.github.aplcornell.viaduct.syntax.datatypes.ClassName
import io.github.aplcornell.viaduct.syntax.datatypes.Get
import io.github.aplcornell.viaduct.syntax.datatypes.ImmutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.MutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.Vector
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.DowngradeNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.InputNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.LiteralNode
import io.github.aplcornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutputNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.SimpleStatementNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.syntax.values.ByteVecValue
import io.github.aplcornell.viaduct.syntax.values.IntegerValue
import io.github.aplcornell.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import mu.KotlinLogging
import java.util.Stack

private val logger = KotlinLogging.logger("Commitment")

/** Commitment protocol interpreter for hash replica hosts. */
class CommitmentProtocolHashReplicaInterpreter(
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val runtime: ViaductProcessRuntime,
) : SingleProtocolInterpreter<CommitmentProtocolHashReplicaInterpreter.CommitmentObject>(
    program,
    runtime.projection.protocol,
) {
    override val availableProtocols: Set<Protocol> =
        setOf(runtime.projection.protocol)

    private val cleartextHost: Host = (runtime.projection.protocol as Commitment).cleartextHost
    private val nullObject = CommitmentCell(Hashing.deterministicHash(IntegerValue(0)).hash)

    private val hashTempStoreStack: Stack<PersistentMap<Temporary, List<Byte>>> = Stack()
    private val ctTempStoreStack: Stack<PersistentMap<Temporary, Value>> = Stack()

    private var hashTempStore: PersistentMap<Temporary, List<Byte>>
        get() {
            return hashTempStoreStack.peek()
        }
        set(value) {
            hashTempStoreStack.pop()
            hashTempStoreStack.push(value)
        }

    private var ctTempStore: PersistentMap<Temporary, Value>
        get() {
            return ctTempStoreStack.peek()
        }
        set(value) {
            ctTempStoreStack.pop()
            ctTempStoreStack.push(value)
        }

    init {
        assert(runtime.projection.protocol is Commitment)

        objectStoreStack.push(persistentMapOf())
        hashTempStoreStack.push(persistentMapOf())
        ctTempStoreStack.push(persistentMapOf())
    }

    private fun pushContext(
        newObjectStore: PersistentMap<ObjectVariable, ObjectLocation>,
        newHashTempStore: PersistentMap<Temporary, List<Byte>>,
        newCtTempStore: PersistentMap<Temporary, Value>,
    ) {
        objectStoreStack.push(newObjectStore)
        hashTempStoreStack.push(newHashTempStore)
        ctTempStoreStack.push(newCtTempStore)
    }

    override suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf(), persistentMapOf())
    }

    override suspend fun pushContext() {
        pushContext(this.objectStore, this.hashTempStore, this.ctTempStore)
    }

    override suspend fun popContext() {
        objectStoreStack.pop()
        hashTempStoreStack.pop()
        ctTempStoreStack.pop()
    }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): CommitmentObject {
        return CommitmentCell(runExpr(expr))
    }

    override suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>,
    ): CommitmentObject {
        return when (className) {
            ImmutableCell, MutableCell -> CommitmentCell(runExpr(arguments[0]))

            Vector -> {
                val length = (runCleartextExpr(arguments[0]) as IntegerValue).value
                CommitmentVector(length, nullObject.bytes)
            }

            else ->
                throw ViaductInterpreterError("Commitment: cannot build object of unknown class $className")
        }
    }

    override fun getNullObject(): CommitmentObject {
        return nullObject
    }

    fun runRead(read: ReadNode): List<Byte> =
        hashTempStore[read.temporary.value]
            ?: throw ViaductInterpreterError(
                "${runtime.projection.protocol.toDocument().print()}:" +
                    " could not find local temporary ${read.temporary.value}",
            )

    private fun runCleartextExpr(expr: AtomicExpressionNode): Value =
        when (expr) {
            is LiteralNode -> expr.value
            is ReadNode -> ctTempStore[expr.temporary.value]!!
        }

    private fun runExpr(expr: ExpressionNode): List<Byte> =
        when (expr) {
            is LiteralNode -> throw ViaductInterpreterError("Commitment: Cannot commit literals")

            is ReadNode -> runRead(expr)

            is DowngradeNode -> runExpr(expr.expression)

            is QueryNode -> {
                val loc = getObjectLocation(expr.variable.value)
                getObject(loc).query(expr.query, expr.arguments)
            }

            is OperatorApplicationNode ->
                throw ViaductInterpreterError("Commitment: cannot perform operations on committed values")

            is InputNode ->
                throw ViaductInterpreterError("Commitment: cannot perform I/O in non-local protocol")
        }

    override suspend fun runLet(stmt: LetNode) {
        val commitment = runExpr(stmt.value)
        hashTempStore = hashTempStore.put(stmt.name.value, commitment)
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        getObject(getObjectLocation(stmt.variable.value)).update(stmt.update, stmt.arguments)
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw ViaductInterpreterError("Commitment: cannot perform I/O in non-local protocol")
    }

    override suspend fun runGuard(expr: AtomicExpressionNode): Value {
        throw ViaductInterpreterError("Commitment: cannot use committed value as a guard")
    }

    override suspend fun runSend(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication,
    ) {
        if (!availableProtocols.contains(recvProtocol)) {
            val relevantEvents: Set<CommunicationEvent> =
                events.getProjectionSends(runtime.projection, Commitment.OPEN_COMMITMENT_OUTPUT)

            val commitment = hashTempStore[sender.name.value]!!
            val commitmentValue = ByteVecValue(commitment)
            for (event in relevantEvents) {
                runtime.send(commitmentValue, event)

                logger.info {
                    "sent opened commitment for ${sender.name.value.name} to " +
                        "${event.recv.protocol.toDocument().print()}@${event.recv.host.name}"
                }
            }
        }
    }

    override suspend fun runReceive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication,
    ) {
        if (sendProtocol != runtime.projection.protocol) { // receive commitment
            when {
                events.any { event -> event.recv.id == Commitment.CLEARTEXT_INPUT } -> {
                    val relevantEvents =
                        events.getHostReceives(runtime.projection.host, Commitment.CLEARTEXT_INPUT)
                    for (event in relevantEvents) {
                        val v: Value = runtime.receive(event)
                        ctTempStore = ctTempStore.put(sender.name.value, v)
                    }
                }

                else -> { // create commitment; receive from committer
                    val commitment: Value =
                        runtime.receive(ProtocolProjection(runtime.projection.protocol, cleartextHost))
                    val committedValue = (commitment as ByteVecValue).value

                    logger.info { "received commitment for ${sender.name.value.name} from host ${cleartextHost.name}" }

                    hashTempStore = hashTempStore.put(sender.name.value, committedValue)
                }
            }
        }
    }

    abstract class CommitmentObject {
        abstract fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): List<Byte>
        abstract fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>)
    }

    inner class CommitmentCell(var bytes: List<Byte>) : CommitmentObject() {
        override fun query(
            query: QueryNameNode,
            @Suppress("UNUSED_PARAMETER") arguments: List<AtomicExpressionNode>,
        ): List<Byte> {
            return when (query.value) {
                is Get -> bytes

                else ->
                    throw ViaductInterpreterError("Commitment: unknown query for commitment cell", query)
            }
        }

        override fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            when (update.value) {
                is io.github.aplcornell.viaduct.syntax.datatypes.Set -> {
                    bytes = runExpr(arguments[0])
                }

                else ->
                    throw ViaductInterpreterError("Commitment: unknown update for commitment cell", update)
            }
        }
    }

    inner class CommitmentVector(val size: Int, defaultValue: List<Byte>) : CommitmentObject() {
        private val commitments: ArrayList<List<Byte>> = ArrayList(size)

        init {
            for (i: Int in 0 until size) {
                commitments.add(defaultValue)
            }
        }

        override fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): List<Byte> {
            return when (query.value) {
                is Get -> {
                    val index = (runCleartextExpr(arguments[0]) as IntegerValue).value
                    commitments[index]
                }

                else ->
                    throw ViaductInterpreterError("Commitment: Unknown query for commitment vector", query)
            }
        }

        override fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            when (update.value) {
                is io.github.aplcornell.viaduct.syntax.datatypes.Set -> {
                    val index = (runCleartextExpr(arguments[0]) as IntegerValue).value
                    commitments[index] = runExpr(arguments[1])
                }

                else ->
                    throw ViaductInterpreterError("Commitment: unknown update for commitment vector", update)
            }
        }
    }
}
