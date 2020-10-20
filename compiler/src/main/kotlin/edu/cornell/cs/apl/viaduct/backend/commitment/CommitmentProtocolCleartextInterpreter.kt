package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.AbstractProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ImmutableCellObject
import edu.cornell.cs.apl.viaduct.backend.MutableCellObject
import edu.cornell.cs.apl.viaduct.backend.NullObject
import edu.cornell.cs.apl.viaduct.backend.ObjectLocation
import edu.cornell.cs.apl.viaduct.backend.PlaintextClassObject
import edu.cornell.cs.apl.viaduct.backend.ProtocolProjection
import edu.cornell.cs.apl.viaduct.backend.VectorObject
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

data class Hashed<T>(val value: T, val info: HashInfo)

/** Commitment protocol interpreter for hosts holding the cleartext value. */
class CommitmentProtocolCleartextInterpreter(
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val runtime: ViaductProcessRuntime
) : AbstractProtocolInterpreter<Hashed<PlaintextClassObject>>(program) {
    private val hashHosts: Set<Host> = (runtime.projection.protocol as Commitment).hashHosts
    private val nullObject: Hashed<PlaintextClassObject> =
        Hashed(NullObject, Hashing.deterministicHash(IntegerValue(0)))

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

    override fun getNullObject(): Hashed<PlaintextClassObject> {
        return nullObject
    }

    /** Send commitment to hash hosts. */
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
        }
    }

    private suspend fun runRead(read: ReadNode): Hashed<Value> {
        val storeValue = tempStore[read.temporary.value]
        return if (storeValue == null) {
            val sendProtocol = protocolAnalysis.primaryProtocol(read)
            if (sendProtocol != runtime.projection.protocol) {
                // TODO: use protocol composer
                val recvValue: Value =
                    runtime.receive(ProtocolProjection(sendProtocol, runtime.projection.host))

                val hashInfo: HashInfo = Hashing.generateHash(recvValue)
                sendCommitment(hashInfo)
                val hashedValue = Hashed(recvValue, hashInfo)
                tempStore.put(read.temporary.value, hashedValue)
                hashedValue
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

    private fun runQuery(query: QueryNode): Hashed<Value> {
        val queryName = query.query.value
        if (queryName != Get) {
            throw ViaductInterpreterError("Commitment: unspported query: $queryName")
        }

        val hashedObj = getObject(getObjectLocation(query.variable.value))
        return when (val obj = hashedObj.value) {
            is ImmutableCellObject -> Hashed(obj.value, hashedObj.info)
            is MutableCellObject -> Hashed(obj.value, hashedObj.info)
            is VectorObject -> TODO("digest for vector")
            else -> throw ViaductInterpreterError("Commitment: query on object of unknown class $obj")
        }
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

    private suspend fun sendCommmitmentObject(obj: PlaintextClassObject): Hashed<PlaintextClassObject> {
        val hashInfo = Hashing.generateHash(obj)
        sendCommitment(hashInfo)

        return Hashed(obj, hashInfo)
    }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): Hashed<PlaintextClassObject> {
        return sendCommmitmentObject(ImmutableCellObject(runExpr(expr).value))
    }

    override suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): Hashed<PlaintextClassObject> {
        val argumentValues: List<Value> = arguments.map { arg -> runExpr(arg).value }
        val obj: PlaintextClassObject =
            when (className) {
                ImmutableCell -> ImmutableCellObject(argumentValues[0])

                MutableCell -> MutableCellObject(argumentValues[0])

                Vector -> {
                    TODO("Commitment for vectors")
                }

                else ->
                    throw ViaductInterpreterError("Commitment: cannot build object of unknown class $className")
            }

        return sendCommmitmentObject(obj)
    }

    override suspend fun runLet(stmt: LetNode) {
        val rhsValue: Hashed<Value> = runExpr(stmt.value)
        tempStore = tempStore.put(stmt.temporary.value, rhsValue)

        // broadcast to readers
        val recvProtocols: List<Protocol> =
            protocolAnalysis.directReaders(stmt).filter { it != runtime.projection.protocol }

        // TODO: use the protocol composer
        if (recvProtocols.isNotEmpty()) {
            for (recvProtocol in recvProtocols) {
                for (recvHost in recvProtocol.hosts) {
                    // send nonce and the opened value
                    val recvProjection = ProtocolProjection(recvProtocol, recvHost)
                    val nonce = ByteVecValue(rhsValue.info.nonce)

                    runtime.send(nonce, recvProjection)
                    runtime.send(rhsValue.value, recvProjection)
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

    override suspend fun runExprAsValue(expr: AtomicExpressionNode): Value {
        throw ViaductInterpreterError("Commitment: cannot use committed value as a guard")
    }
}
