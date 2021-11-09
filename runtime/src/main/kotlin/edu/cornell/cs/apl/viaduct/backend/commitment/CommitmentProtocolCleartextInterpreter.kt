package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.ObjectLocation
import edu.cornell.cs.apl.viaduct.backend.SingleProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.backends.commitment.Commitment
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
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

private val logger = KotlinLogging.logger("Commitment")

data class Hashed<T>(val value: T, val info: HashInfo)

/** Commitment protocol interpreter for hosts holding the cleartext value. */
class CommitmentProtocolCleartextInterpreter(
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val runtime: ViaductProcessRuntime
) : SingleProtocolInterpreter<CommitmentProtocolCleartextInterpreter.HashedObject>(
    program,
    runtime.projection.protocol
) {
    private val hashHosts: Set<Host> = (runtime.projection.protocol as Commitment).hashHosts

    private val tempStoreStack: Stack<PersistentMap<Temporary, Hashed<Value>>> = Stack()
    private val ctTempStoreStack: Stack<PersistentMap<Temporary, Value>> = Stack()

    private var tempStore: PersistentMap<Temporary, Hashed<Value>>
        get() {
            return tempStoreStack.peek()
        }
        set(value) {
            tempStoreStack.pop()
            tempStoreStack.push(value)
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
        tempStoreStack.push(persistentMapOf())
        ctTempStoreStack.push(persistentMapOf())
    }

    private fun pushContext(
        newObjectStore: PersistentMap<ObjectVariable, ObjectLocation>,
        newTempStore: PersistentMap<Temporary, Hashed<Value>>,
        newCtTempStore: PersistentMap<Temporary, Value>
    ) {
        objectStoreStack.push(newObjectStore)
        tempStoreStack.push(newTempStore)
        ctTempStoreStack.push(newCtTempStore)
    }

    override suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf(), persistentMapOf())
    }

    override suspend fun pushContext() {
        pushContext(this.objectStore, this.tempStore, this.ctTempStore)
    }

    override suspend fun popContext() {
        objectStoreStack.pop()
        tempStoreStack.pop()
        ctTempStoreStack.pop()
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

    private fun runCleartextExpr(expr: AtomicExpressionNode): Value =
        when (expr) {
            is LiteralNode -> expr.value
            is ReadNode -> ctTempStore[expr.temporary.value]!!
        }

    private fun runExpr(expr: ExpressionNode): Hashed<Value> =
        when (expr) {
            is LiteralNode -> Hashed(expr.value, Hashing.deterministicHash(expr.value))

            is ReadNode -> tempStore[expr.temporary.value]!!

            is DowngradeNode -> runExpr(expr.expression)

            is QueryNode ->
                getObject(getObjectLocation(expr.variable.value)).query(expr.query, expr.arguments)

            is OperatorApplicationNode ->
                throw ViaductInterpreterError("Commitment: cannot perform operations on committed values")

            is InputNode ->
                throw ViaductInterpreterError("Commitment: cannot perform I/O in non-local protocol")
        }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): HashedObject {
        return HashedCellObject(runExpr(expr))
    }

    override suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): HashedObject {
        return when (className) {
            ImmutableCell, MutableCell -> HashedCellObject(runExpr(arguments[0]))
            Vector -> {
                val length = (runCleartextExpr(arguments[0]) as IntegerValue).value
                HashedVectorObject(length, HashedNullObject.hashed)
            }
            else -> throw ViaductInterpreterError("Commitment: cannot build object of unknown class $className")
        }
    }

    override suspend fun runLet(stmt: LetNode) {
        val rhsValue: Hashed<Value> = runExpr(stmt.value)
        tempStore = tempStore.put(stmt.temporary.value, rhsValue)
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        getObject(getObjectLocation(stmt.variable.value)).update(stmt.update, stmt.arguments)
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
                    "sent opened value and nonce for ${sender.temporary.value.name} to " +
                        "${event.recv.protocol.asDocument().print()}@${event.recv.host.name}"
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
            when {
                events.any { event -> event.recv.id == Commitment.CLEARTEXT_INPUT } -> { // cleartext input
                    val relevantEvents =
                        events.getHostReceives(runtime.projection.host, Commitment.CLEARTEXT_INPUT)

                    var cleartextValue: Value? = null
                    for (event in relevantEvents) {
                        val receivedValue: Value = runtime.receive(event)

                        if (cleartextValue != null) {
                            if (receivedValue != cleartextValue) {
                                throw ViaductInterpreterError("Commitment: Received different value")
                            }
                        } else {
                            cleartextValue = receivedValue
                        }
                    }

                    ctTempStore = ctTempStore.put(sender.temporary.value, cleartextValue!!)
                }

                else -> { // create commitment
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

                    for (hashHost in hashHosts) {
                        runtime.send(commitment, ProtocolProjection(runtime.projection.protocol, hashHost))
                        logger.info { "sent commitment for ${sender.temporary.value.name} to host ${hashHost.name}" }
                    }

                    val hashedValue = Hashed(cleartextValue, hashInfo)
                    tempStore = tempStore.put(sender.temporary.value, hashedValue)
                }
            }
        }
    }

    abstract class HashedObject {
        abstract fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): Hashed<Value>
        abstract fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>)
    }

    object HashedNullObject : HashedObject() {
        val hashed: Hashed<Value> = Hashed(IntegerValue(0), Hashing.generateHash(IntegerValue(0)))

        override fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): Hashed<Value> {
            throw ViaductInterpreterError("Commitment: unknown query for null object", query)
        }

        override fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            throw ViaductInterpreterError("Commitment: unknown update for null object", update)
        }
    }

    inner class HashedCellObject(var value: Hashed<Value>) : HashedObject() {
        override fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): Hashed<Value> {
            return when (query.value) {
                is Get -> this.value
                else -> throw ViaductInterpreterError("Commitment: unknown query for cell object", query)
            }
        }

        override fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            when (update.value) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    this.value = runExpr(arguments[0])
                }

                else -> throw ViaductInterpreterError("Commitment: unknown update for cell object", update)
            }
        }
    }

    inner class HashedVectorObject(val size: Int, defaultValue: Hashed<Value>) : HashedObject() {
        private val hashedObjects = ArrayList<Hashed<Value>>(size)

        init {
            for (i: Int in 0 until size) {
                hashedObjects.add(defaultValue)
            }
        }

        override fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): Hashed<Value> {
            return when (query.value) {
                is Get -> {
                    val index = (runCleartextExpr(arguments[0]) as IntegerValue).value
                    this.hashedObjects[index]
                }
                else -> throw ViaductInterpreterError("Commitment: unknown query for cell object", query)
            }
        }

        override fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            when (update.value) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    val index = (runCleartextExpr(arguments[0]) as IntegerValue).value
                    this.hashedObjects[index] = runExpr(arguments[1])
                }

                else -> throw ViaductInterpreterError("Commitment: unknown update for cell object", update)
            }
        }
    }
}
