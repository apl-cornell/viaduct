package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.ObjectLocation
import edu.cornell.cs.apl.viaduct.backend.ProtocolProjection
import edu.cornell.cs.apl.viaduct.backend.SingleProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
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
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Commitment")

data class Hashed<T>(val value: T, val info: HashInfo)

abstract class HashedObject {
    abstract fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): Hashed<Value>
}

object HashedNullObject : HashedObject() {
    override fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): Hashed<Value> {
        throw ViaductInterpreterError("Commitment: unknown query ${query.value} for null object")
    }
}

data class HashedCellObject(val value: Hashed<Value>) : HashedObject() {
    override fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): Hashed<Value> {
        return when (query.value) {
            is Get -> this.value
            else -> throw ViaductInterpreterError("Commitment: unknown query ${query.value} for cell object")
        }
    }
}

/** Commitment protocol interpreter for hosts holding the cleartext value. */
class CommitmentProtocolCleartextInterpreter(
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val runtime: ViaductProcessRuntime
) : SingleProtocolInterpreter<HashedObject>(program, runtime.projection.protocol) {
    private val hashHosts: Set<Host> = (runtime.projection.protocol as Commitment).hashHosts

    private val tempStoreStack: Stack<PersistentMap<Temporary, Hashed<Value>>> = Stack()

    private var tempStore: PersistentMap<Temporary, Hashed<Value>>
        get() {
            return tempStoreStack.peek()
        }
        set(value) {
            tempStoreStack.pop()
            tempStoreStack.push(value)
        }

    init {
        assert(runtime.projection.protocol is Commitment)

        objectStoreStack.push(persistentMapOf())
        tempStoreStack.push(persistentMapOf())
    }

    private fun pushContext(
        newObjectStore: PersistentMap<ObjectVariable, ObjectLocation>,
        newTempStore: PersistentMap<Temporary, Hashed<Value>>
    ) {
        objectStoreStack.push(newObjectStore)
        tempStoreStack.push(newTempStore)
    }

    override suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf())
    }

    override suspend fun pushContext() {
        pushContext(this.objectStore, this.tempStore)
    }

    override suspend fun popContext() {
        objectStoreStack.pop()
        tempStoreStack.pop()
    }

    override fun getNullObject(): HashedObject = HashedNullObject

    private suspend fun sendCommitment(hashInfo: HashInfo) {
        val commitment = ByteVecValue(hashInfo.hash)
        for (commitmentReceiver: Host in hashHosts) {
            runtime.send(
                commitment,
                ProtocolProjection(
                    runtime.projection.protocol,
                    commitmentReceiver
                )
            )

            logger.info { "sent commitment to host ${commitmentReceiver.name}" }
        }
    }

    private fun runRead(read: ReadNode): Hashed<Value> =
        tempStore[read.temporary.value]
            ?: throw ViaductInterpreterError(
                "${runtime.projection.protocol.asDocument.print()}:" +
                    " could not find local temporary ${read.temporary.value}"
            )

    private fun runQuery(query: QueryNode): Hashed<Value> {
        return getObject(getObjectLocation(query.variable.value)).query(query.query, query.arguments)
    }

    private suspend fun runExpr(expr: ExpressionNode): Hashed<Value> =
        when (expr) {
            is LiteralNode -> Hashed(expr.value, Hashing.deterministicHash(expr.value))

            is ReadNode -> runRead(expr)

            is DowngradeNode -> runExpr(expr.expression)

            is QueryNode -> runQuery(expr)

            is OperatorApplicationNode ->
                throw ViaductInterpreterError("Commitment: cannot perform operations on committed values")

            is InputNode ->
                throw ViaductInterpreterError("Commitment: cannot perform I/O in non-local protocol")

            is ReceiveNode ->
                throw IllegalInternalCommunicationError(expr)
        }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): HashedObject {
        return HashedCellObject(runExpr(expr))
    }

    override suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): HashedObject {
        val argumentValues: List<Hashed<Value>> = arguments.map { arg -> runExpr(arg) }
        return when (className) {
            ImmutableCell, MutableCell -> HashedCellObject(argumentValues[0])
            Vector -> TODO("Commitment for vectors")
            else -> throw ViaductInterpreterError("Commitment: cannot build object of unknown class $className")
        }
    }

    override suspend fun runLet(stmt: LetNode) {
        val rhsValue: Hashed<Value> = runExpr(stmt.value)
        tempStore = tempStore.put(stmt.temporary.value, rhsValue)
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        throw ViaductInterpreterError("Commitment: cannot perform update on committed value")
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw ViaductInterpreterError("Commitment: cannot perform I/O in non-local protocol")
    }

    override suspend fun runGuard(expr: AtomicExpressionNode): Value {
        return runExpr(expr).value
    }

    override suspend fun runSend(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ) {
        if (receiver != runtime.projection.protocol) {
            val hashedValue: Hashed<Value> = tempStore[sender.temporary.value]!!

            val relevantEvents: Set<CommunicationEvent> =
                events.getProjectionSends(runtime.projection, Commitment.OPEN_CLEARTEXT_OUTPUT)

            val nonce = ByteVecValue(hashedValue.info.nonce)
            for (event in relevantEvents) {
                // send nonce and the opened value
                val recvProjection = event.recv.asProjection()
                runtime.send(nonce, recvProjection)
                runtime.send(hashedValue.value, recvProjection)

                logger.info {
                    "sent opened value and nonce to " +
                        "${event.recv.protocol.asDocument.print()}@${event.recv.host.name}"
                }
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
        if (sendProtocol != runtime.projection.protocol) {
            // receive cleartext inputs
            val cleartextInputEvents: Set<CommunicationEvent> =
                events.getProjectionReceives(runtime.projection, Commitment.INPUT)

            var cleartextValue: Value? = null
            for (event in cleartextInputEvents) {
                val recvValue: Value = runtime.receive(event)

                when {
                    cleartextValue == null -> {
                        cleartextValue = recvValue
                    }

                    cleartextValue != recvValue -> {
                        throw ViaductInterpreterError("Commitment: received different cleartext values")
                    }
                }
            }

            assert(cleartextValue != null)

            // send commitment to hash hosts
            val hashInfo: HashInfo = Hashing.generateHash(cleartextValue!!)
            val commitment = ByteVecValue(hashInfo.hash)

            val createCommitmentEvents: Set<CommunicationEvent> =
                events.getProjectionSends(runtime.projection, Commitment.CREATE_COMMITMENT_OUTPUT)

            for (event in createCommitmentEvents) {
                runtime.send(commitment, event)
                logger.info { "sent commitment to host ${event.recv.host.name}" }
            }

            val hashedValue = Hashed(cleartextValue, hashInfo)
            tempStore.put(sender.temporary.value, hashedValue)
        }
    }
}
