package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.AbstractBackendInterpreter
import edu.cornell.cs.apl.viaduct.backend.ImmutableCellObject
import edu.cornell.cs.apl.viaduct.backend.MutableCellObject
import edu.cornell.cs.apl.viaduct.backend.PlaintextClassObject
import edu.cornell.cs.apl.viaduct.backend.ProtocolProjection
import edu.cornell.cs.apl.viaduct.backend.VectorObject
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import java.util.Stack

data class Hashed<T>(val value: T, val info: HashInfo)

internal class CommitmentCleartext(
    private val runtime: ViaductProcessRuntime,
    private val typeAnalysis: TypeAnalysis,
    private val hashHosts: Set<Host>
) : AbstractBackendInterpreter() {

    private val projection: ProtocolProjection = runtime.projection

    private val tempStack: Stack<PersistentMap<Temporary, Hashed<Value>>> = Stack()

    private var tempStore: PersistentMap<Temporary, Hashed<Value>>
        get() {
            return tempStack.peek()
        }
        set(value) {
            tempStack.pop()
            tempStack.push(value)
        }

    private val objectStoreStack: Stack<PersistentMap<ObjectVariable, Hashed<PlaintextClassObject>>> = Stack()

    private var objectStore: PersistentMap<ObjectVariable, Hashed<PlaintextClassObject>>
        get() {
            return objectStoreStack.peek()
        }
        set(value) {
            objectStoreStack.pop()
            objectStoreStack.push(value)
        }

    init {
        tempStack.push(persistentMapOf())
        objectStoreStack.push(persistentMapOf())
    }

    override fun pushContext() {
        tempStack.push(tempStore)
        objectStoreStack.push(objectStore)
    }

    override fun popContext() {
        tempStack.pop()
        objectStoreStack.pop()
    }

    override fun getContextMarker(): Int {
        return tempStack.size
    }

    override fun restoreContext(marker: Int) {
        while (tempStack.size > marker) {
            tempStack.pop()
            objectStoreStack.pop()
        }
    }

    private fun runAtomicExprHashed(expr: AtomicExpressionNode): Hashed<Value> {
        return when (expr) {
            is LiteralNode -> Hashed(expr.value, Hashing.deterministicHash(expr.value))
            is ReadNode -> (tempStore[expr.temporary.value]
                ?: throw UndefinedNameError(expr.temporary))
        }
    }

    override suspend fun runAtomicExpr(expr: AtomicExpressionNode): Value {
        return runAtomicExprHashed(expr).value
    }

    override suspend fun runDeclaration(stmt: DeclarationNode) {
        val argumentValues: List<Value> = stmt.arguments.map { arg -> runAtomicExpr(arg) }
        val obj: PlaintextClassObject
        obj = when (val objectType = typeAnalysis.type(stmt)) {
            is ImmutableCellType -> {
                ImmutableCellObject(argumentValues[0], stmt.variable, objectType)
            }
            is MutableCellType -> {
                MutableCellObject(argumentValues[0], stmt.variable, objectType)
            }
            is VectorType -> {
                TODO("Commitment for vectors")
            }
            else -> {
                TODO("other")
            }
        }
        val hashInfo = Hashing.generateHash(obj)
        objectStore = objectStore.put(
            stmt.variable.value,
            Hashed(obj, hashInfo)
        )
        for (commitmentReceiver: Host in hashHosts) {
            runtime.send(
                ByteVecValue(hashInfo.hash),
                ProtocolProjection(
                    projection.protocol,
                    commitmentReceiver
                )
            )
        }
    }

    private fun runQuery(q: QueryNode): Hashed<Value> {
        val o = objectStore[q.variable.value] ?: throw Error("bad lookup")
        return when (o.value) {
            // We query an immutable cell by just copying the hash for the value.
            is ImmutableCellObject -> Hashed(o.value.value, o.info)

            // We query an immutable cell by just copying the hash for the value.
            is MutableCellObject -> Hashed(o.value.value, o.info)

            // TODO: in this case, we need to come up with a hash for an element from hash of collection.
            // This will require us to modify HashInfo to support a collection of hashes (perhaps a Merkle proof)
            is VectorObject -> TODO("Digest for vector")
        }
    }

    override suspend fun runLet(stmt: LetNode) {
        when (val rhs = stmt.value) {
            is ReceiveNode -> {
                val sendProtocol = rhs.protocol.value
                val v = runtime.receive(
                    ProtocolProjection(
                        sendProtocol,
                        projection.host
                    )
                )
                val hashInfo = Hashing.generateHash(v)
                tempStore = tempStore.put(stmt.temporary.value, Hashed(v, hashInfo))
                // send hash to all receivers (currently dummy)
                for (commitmentReceiver: Host in hashHosts) {
                    runtime.send(
                        ByteVecValue(hashInfo.hash),
                        ProtocolProjection(
                            projection.protocol,
                            commitmentReceiver
                        )
                    )
                }
            }
            is AtomicExpressionNode -> {
                tempStore = tempStore.put(stmt.temporary.value, runAtomicExprHashed(rhs))
            }
            is DowngradeNode -> {
                tempStore = tempStore.put(stmt.temporary.value, runAtomicExprHashed(rhs.expression))
            }
            is QueryNode -> {
                tempStore = tempStore.put(stmt.temporary.value, runQuery(rhs))
            }
            else -> throw Exception("Unsupported node in CommitmentSender.runLet: $rhs")
        }
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw Exception("No output in commitment")
    }

    override suspend fun runSend(stmt: SendNode) {
        val hashedMessage = runAtomicExprHashed(stmt.message)

        // Send the nonce and then the value
        for (recvHost: Host in stmt.protocol.value.hosts) {
            runtime.send(ByteVecValue(hashedMessage.info.nonce),
                ProtocolProjection(stmt.protocol.value, recvHost)
            )
            runtime.send(hashedMessage.value,
                ProtocolProjection(stmt.protocol.value, recvHost)
            )
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        throw Exception("No updates in commitment")
    }
}
