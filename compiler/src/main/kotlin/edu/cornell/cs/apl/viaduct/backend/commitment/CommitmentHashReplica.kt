package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.AbstractBackendInterpreter
import edu.cornell.cs.apl.viaduct.backend.ProtocolProjection
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
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

data class ObjectHash(val type: ObjectType, val hash: List<Byte>)

internal class CommitmentHashReplica(
    private val runtime: ViaductProcessRuntime,
    private val typeAnalysis: TypeAnalysis,
    private val cleartextHost: Host
) : AbstractBackendInterpreter() {

    private val projection: ProtocolProjection = runtime.projection

    private val ctTempStack: Stack<PersistentMap<Temporary, Value>> = Stack()

    private var ctTempStore: PersistentMap<Temporary, Value>
        get() {
            return ctTempStack.peek()
        }
        set(value) {
            ctTempStack.pop()
            ctTempStack.push(value)
        }

    private val hashTempStack: Stack<PersistentMap<Temporary, List<Byte>>> = Stack()

    private var hashTempStore: PersistentMap<Temporary, List<Byte>>
        get() {
            return hashTempStack.peek()
        }
        set(value) {
            hashTempStack.pop()
            hashTempStack.push(value)
        }

    private val hashObjectStack: Stack<PersistentMap<ObjectVariable, ObjectHash>> = Stack()

    private var hashObjectStore: PersistentMap<ObjectVariable, ObjectHash>
        get() {
            return hashObjectStack.peek()
        }
        set(value) {
            hashObjectStack.pop()
            hashObjectStack.push(value)
        }

    init {
        ctTempStack.push(persistentMapOf())
        hashTempStack.push(persistentMapOf())

        hashObjectStack.push(persistentMapOf())
    }

    override fun pushContext() {
        ctTempStack.push(ctTempStore)
        hashTempStack.push(hashTempStore)
        hashObjectStack.push(persistentMapOf())
    }

    override fun popContext() {
        ctTempStack.pop()
        hashTempStack.pop()
        hashObjectStack.pop()
    }

    override fun getContextMarker(): Int {
        return ctTempStack.size
    }

    override fun restoreContext(marker: Int) {
        while (ctTempStack.size > marker) {
            ctTempStack.pop()
            hashTempStack.pop()
            hashObjectStack.pop()
        }
    }

    /** NOTE: control flow can either get here from being in cleartext, or it can return a hash.
     *
     */
    override suspend fun runAtomicExpr(expr: AtomicExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value
            is ReadNode -> {
                if (ctTempStore.containsKey(expr.temporary.value)) {
                    ctTempStore[expr.temporary.value]
                        ?: throw UndefinedNameError(expr.temporary)
                } else {
                    ByteVecValue(
                        hashTempStore[expr.temporary.value]
                            ?: throw UndefinedNameError(expr.temporary)
                    )
                }
            }
        }
    }

    override suspend fun runDeclaration(stmt: DeclarationNode) {
        val hash = runtime.receive(
            ProtocolProjection(
                projection.protocol,
                cleartextHost
            )
        )
        hashObjectStore = hashObjectStore.put(
            stmt.variable.value,
            ObjectHash(typeAnalysis.type(stmt), (hash as ByteVecValue).value)
        )
    }

    private suspend fun runRead(tempFrom: Temporary, tempTo: Temporary) {
        if (ctTempStore.containsKey(tempFrom)) {
            ctTempStore = ctTempStore.put(
                tempTo,
                ctTempStore[tempFrom]!!
            )
        } else { // thing to read is a hash; copy the hash
            hashTempStore = hashTempStore.put(
                tempTo,
                hashTempStore[tempFrom] ?: throw Exception("bad hash read")
            )
        }
    }

    private fun runQuery(q: QueryNode): List<Byte> {
        val objhash = hashObjectStore[q.variable.value] ?: throw Error("Hashed object not found")
        return when (objhash.type) {
            is ImmutableCellType -> objhash.hash
            is MutableCellType -> objhash.hash
            is VectorType -> TODO("Vector hashing")
            else -> throw Error("Unexpected object type: ${objhash.type}")
        }
    }

    override suspend fun runLet(stmt: LetNode) {
        when (val rhs = stmt.value) {
            is ReceiveNode -> {
                // receive the commitment from sender
                val h = runtime.receive(
                    ProtocolProjection(
                        projection.protocol,
                        cleartextHost
                    )
                )
                hashTempStore = hashTempStore.put(stmt.temporary.value, (h as ByteVecValue).value)
            }
            is AtomicExpressionNode -> {
                when (rhs) {
                    is LiteralNode -> ctTempStore = ctTempStore.put(stmt.temporary.value, rhs.value)
                    is ReadNode -> runRead(rhs.temporary.value, stmt.temporary.value)
                }
            }
            is DowngradeNode -> {
                when (val e = rhs.expression) {
                    is LiteralNode -> ctTempStore = ctTempStore.put(stmt.temporary.value, e.value)
                    is ReadNode -> runRead(e.temporary.value, stmt.temporary.value)
                }
            }
            is QueryNode -> {
                // TODO: This is standing in for the commitment receiver sending various proofs for
                // a query, such as a Merkle proof for an array lookup.
                hashTempStore = hashTempStore.put(stmt.temporary.value, runQuery(rhs))
            }

            else -> throw Exception("Unsupported node in CommitmentReceiver.runLet: $rhs")
        }
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw Exception("No output in commitment")
    }

    override suspend fun runSend(stmt: SendNode) {
        val msg = runAtomicExpr(stmt.message)

        // send to all recvers
        for (recvHost: Host in stmt.protocol.value.hosts) {
            runtime.send(
                msg,
                ProtocolProjection(stmt.protocol.value, recvHost)
            )
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        throw Exception("Update for commitment")
    }
}
