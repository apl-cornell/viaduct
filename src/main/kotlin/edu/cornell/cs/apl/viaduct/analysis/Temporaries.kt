package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.StatementReducer
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.traverse
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentHashSet

/** Returns the set of temporaries read by this expression. */
fun ExpressionNode.reads(): Set<Temporary> =
    this.traverse(Reads)

/** Returns the set of temporaries read by this statement. */
fun StatementNode.reads(): Set<Temporary> =
    this.traverse(Reads)

/** Computes the set of temporaries that appear in a [ReadNode]. */
private object Reads : StatementReducer<PersistentSet<Temporary>> {
    override val initial: PersistentSet<Temporary>
        get() = persistentHashSetOf()

    override val combine: (PersistentSet<Temporary>, PersistentSet<Temporary>) -> PersistentSet<Temporary>
        get() = ::union

    override fun leave(node: ReadNode): PersistentSet<Temporary> =
        persistentHashSetOf(node.temporary.value)
}

/**
 * A map from [Temporary] variables declared in a process to the set of statements that
 * (potentially) read those temporaries. Note that these sets never contain [BlockNode]s.
 */
class Readers
private constructor(
    private val readers: PersistentMap<Temporary, PersistentSet<StatementNode>>
) : Map<Temporary, Set<StatementNode>> by readers {
    /** Constructs the readers map for temporaries in [process]. */
    constructor(process: ProcessDeclarationNode) :
        this(computeReaders(process))

    /**
     * Computes the set of temporaries read by each statement all the while generating
     * a reverse map.
     */
    private class Reads : StatementReducer<PersistentSet<Temporary>> {
        val readers = mutableMapOf<Temporary, MutableSet<StatementNode>>()

        override val initial: PersistentSet<Temporary>
            get() = persistentHashSetOf()

        override val combine: (PersistentSet<Temporary>, PersistentSet<Temporary>) -> PersistentSet<Temporary>
            get() = ::union

        override fun leave(node: ReadNode): PersistentSet<Temporary> =
            persistentHashSetOf(node.temporary.value)

        override fun leave(
            node: BlockNode,
            statements: List<PersistentSet<Temporary>>
        ): PersistentSet<Temporary> {
            // Add a reverse mapping from a statement to each temporary it reads
            node.statements.forEachIndexed { i, statement ->
                if (statement !is BlockNode) {
                    statements[i].forEach {
                        val readersSet = readers.getOrPut(it) { mutableSetOf() }
                        readersSet.add(statement)
                    }
                }
            }
            readers.entries.associateBy { }
            return super.leave(node, statements)
        }
    }

    private companion object {
        fun computeReaders(process: ProcessDeclarationNode):
            PersistentMap<Temporary, PersistentSet<StatementNode>> {
            val reads = Reads()
            process.traverse(reads)
            return reads.readers.mapValues { it.value.toPersistentHashSet() }.toPersistentHashMap()
        }
    }
}

/** A slightly optimized persistent set union. */
private fun <T> union(set1: PersistentSet<T>, set2: PersistentSet<T>): PersistentSet<T> =
    when {
        set1.isEmpty() ->
            set2
        set2.isEmpty() ->
            set1
        set1.size >= set2.size ->
            set1.addAll(set2)
        else ->
            set2.addAll(set1)
    }
