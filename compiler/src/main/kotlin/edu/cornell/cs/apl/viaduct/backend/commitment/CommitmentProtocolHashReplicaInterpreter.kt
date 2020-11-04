package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.ObjectLocation
import edu.cornell.cs.apl.viaduct.backend.SingleProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.UpdateNameNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
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
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Commitment")

class CommitmentObject(val bytes: List<Byte>) {
    fun query(
        query: QueryNameNode,
        @Suppress("UNUSED_PARAMETER") arguments: List<AtomicExpressionNode>
    ): List<Byte> {
        return when (query.value) {
            is Get -> bytes

            else -> {
                throw ViaductInterpreterError("Commitment: unknown query for commitment object", query)
            }
        }
    }

    fun update(
        @Suppress("UNUSED_PARAMETER") update: UpdateNameNode,
        @Suppress("UNUSED_PARAMETER") arguments: List<AtomicExpressionNode>
    ) {
        throw ViaductInterpreterError("Commitment: cannot update committed values")
    }
}

/** Commitment protocol interpreter for hash replica hosts. */
class CommitmentProtocolHashReplicaInterpreter(
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val runtime: ViaductProcessRuntime
) : SingleProtocolInterpreter<CommitmentObject>(program, runtime.projection.protocol) {
    override val availableProtocols: Set<Protocol> =
        setOf(runtime.projection.protocol)

    private val cleartextHost: Host = (runtime.projection.protocol as Commitment).cleartextHost
    private val nullObject = CommitmentObject(Hashing.deterministicHash(IntegerValue(0)).hash)

    private val hashTempStoreStack: Stack<PersistentMap<Temporary, List<Byte>>> = Stack()

    private var hashTempStore: PersistentMap<Temporary, List<Byte>>
        get() {
            return hashTempStoreStack.peek()
        }
        set(value) {
            hashTempStoreStack.pop()
            hashTempStoreStack.push(value)
        }

    init {
        assert(runtime.projection.protocol is Commitment)

        objectStoreStack.push(persistentMapOf())
        hashTempStoreStack.push(persistentMapOf())
    }

    private fun pushContext(
        newObjectStore: PersistentMap<ObjectVariable, ObjectLocation>,
        newTempStore: PersistentMap<Temporary, List<Byte>>
    ) {
        objectStoreStack.push(newObjectStore)
        hashTempStoreStack.push(newTempStore)
    }

    override suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf())
    }

    override suspend fun pushContext() {
        pushContext(this.objectStore, this.hashTempStore)
    }

    override suspend fun popContext() {
        objectStoreStack.pop()
        hashTempStoreStack.pop()
    }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): CommitmentObject {
        return CommitmentObject(runExpr(expr))
    }

    override suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): CommitmentObject {
        val argumentValues = arguments.map { arg -> runExpr(arg) }
        return when (className) {
            ImmutableCell, MutableCell -> CommitmentObject(argumentValues[0])

            Vector -> TODO("Commitment for vectors")

            else ->
                throw ViaductInterpreterError("Commitment: cannot build object of unknown class $className")
        }
    }

    override fun getNullObject(): CommitmentObject {
        return nullObject
    }

    suspend fun runRead(read: ReadNode): List<Byte> {
        val storeValue = hashTempStore[read.temporary.value]

        return if (storeValue == null) {
            val sendProtocol = protocolAnalysis.primaryProtocol(read)
            if (sendProtocol != runtime.projection.protocol) { // receive commitment
                val event =
                    protocolAnalysis
                        .relevantCommunicationEvents(read)
                        .getProjectionReceives(runtime.projection, Commitment.CREATE_COMMITMENT_INPUT)
                        .first()

                logger.info { "expecting to receive " }

                val commitment: Value = runtime.receive(event)
                val committedValue = (commitment as ByteVecValue).value

                logger.info { "received commitment from host ${event.send.host.name}" }

                hashTempStore = hashTempStore.put(read.temporary.value, committedValue)
                committedValue
            } else { // temporary should be stored locally, but isn't
                throw ViaductInterpreterError(
                    "${runtime.projection.protocol.asDocument.print()}:" +
                        " could not find local temporary ${read.temporary.value}"
                )
            }
        } else {
            storeValue
        }
    }

    private suspend fun runExpr(expr: ExpressionNode): List<Byte> =
        when (expr) {
            is LiteralNode -> TODO()

            is ReadNode -> runRead(expr)

            is DowngradeNode -> runExpr(expr.expression)

            is QueryNode -> {
                val loc = getObjectLocation(expr.variable.value)
                getObject(loc).query(expr.query, expr.arguments)
            }

            is OperatorApplicationNode ->
                throw ViaductInterpreterError("Commitment: cannot perform operations on committed values")

            is ReceiveNode ->
                throw IllegalInternalCommunicationError(expr)

            is InputNode ->
                throw ViaductInterpreterError("Commitment: cannot perform I/O in non-local protocol")
        }

    override suspend fun runLet(stmt: LetNode) {
        val commitment = runExpr(stmt.value)
        hashTempStore = hashTempStore.put(stmt.temporary.value, commitment)

        // broadcast to readers
        val readers: Set<SimpleStatementNode> = protocolAnalysis.directRemoteReaders(stmt)

        val commitmentValue = ByteVecValue(commitment)
        for (reader in readers) {
            val events: Set<CommunicationEvent> =
                protocolAnalysis
                    .relevantCommunicationEvents(stmt, reader)
                    .getProjectionSends(runtime.projection, Commitment.OPEN_COMMITMENT_OUTPUT)

            for (event in events) {
                runtime.send(commitmentValue, event)

                logger.info {
                    "sent opened commitment to " +
                        "${event.recv.protocol.asDocument.print()}@${event.recv.host.name}"
                }
            }
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        throw ViaductInterpreterError("Commitment: cannot perform update on committed value")
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw ViaductInterpreterError("Commitment: cannot perform I/O in non-local protocol")
    }

    override suspend fun runGuard(expr: AtomicExpressionNode): Value {
        throw ViaductInterpreterError("Commitment: cannot use committed value as a guard")
    }
}
