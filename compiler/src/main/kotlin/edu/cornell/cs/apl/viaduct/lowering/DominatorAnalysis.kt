package edu.cornell.cs.apl.viaduct.lowering

import edu.cornell.cs.apl.viaduct.algebra.MeetSemiLattice
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowNode
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge
import edu.cornell.cs.apl.viaduct.util.dataflow.solveDataFlow
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.intersect
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import org.jgrapht.graph.DefaultDirectedGraph

private sealed class AbstractDataFlowSet<T, Self : AbstractDataFlowSet<T, Self>> : MeetSemiLattice<Self> {
    abstract val set: PersistentSet<T>
}

/** Powerset element. */
private data class DataFlowSet<T>(override val set: PersistentSet<T>) : AbstractDataFlowSet<T, DataFlowSet<T>>() {
    override fun meet(that: DataFlowSet<T>): DataFlowSet<T> {
        return DataFlowSet(this.set.intersect(that.set))
    }

    override fun lessThanOrEqualTo(that: DataFlowSet<T>): Boolean =
        this.set.containsAll(that.set)
}

/** Inverse powerset element (bottom is universe set, meet is union). */
private data class InvertedDataFlowSet<T>(override val set: PersistentSet<T>) : AbstractDataFlowSet<T, InvertedDataFlowSet<T>>() {
    override fun meet(that: InvertedDataFlowSet<T>): InvertedDataFlowSet<T> {
        return InvertedDataFlowSet(this.set.addAll(that.set))
    }

    override fun lessThanOrEqualTo(that: InvertedDataFlowSet<T>): Boolean =
        that.set.containsAll(this.set)
}

private data class DominatorAnalysisNode<S : AbstractDataFlowSet<RegularBlockLabel, S>>(
    val label: RegularBlockLabel,
    val isEntryPoint: Boolean,
    val nodeBuilder: (PersistentSet<RegularBlockLabel>) -> S
) : DataFlowNode<S> {
    override fun transfer(input: S): S {
        // recall that the top element is all labels; if we don't special case entry points,
        // then the analysis will compute top for all nodes
        return if (isEntryPoint) {
            nodeBuilder(persistentSetOf(label))
        } else {
            nodeBuilder(input.set.add(label))
        }
    }
}

/** Perform (strict) dominator analysis by dataflow analysis.
 *  The lattice is the powerset lattice (top = set of all block labels),
 *  while the transfer function is defined by:
 *  - in(in_nodes) = fold in_nodes TOP (acc in_node -> intersect acc out(in_node))
 *  - out(n) = {n} union in(n)
 *  - out(START) = {START} (important that you special case entry points!)
 *  */
private fun <S : AbstractDataFlowSet<RegularBlockLabel, S>> computeDominators(
    successorMap: Map<RegularBlockLabel, Set<RegularBlockLabel>>,
    entryPoints: Set<RegularBlockLabel>,
    nodeBuilder: (PersistentSet<RegularBlockLabel>) -> S
): Map<RegularBlockLabel, Set<RegularBlockLabel>> {
    val graph = DefaultDirectedGraph<DominatorAnalysisNode<S>, IdentityEdge<S>>(null, null, false)
    val nodeMap = successorMap.keys.associateWith { label ->
        DominatorAnalysisNode(label, entryPoints.contains(label), nodeBuilder)
    }

    for (kv in successorMap) {
        val src = nodeMap[kv.key]!!
        graph.addVertex(src)

        for (successor in kv.value) {
            val dst = nodeMap[successor]!!
            val edge = IdentityEdge<S>()
            graph.addVertex(dst)
            graph.addEdge(src, dst, edge)
        }
    }

    // return blocks that are *strictly* dominated
    return solveDataFlow(nodeBuilder(nodeMap.keys.toPersistentSet()), graph).map { kv ->
        kv.key.label to kv.value.set.filter { v -> v != kv.key.label }.toSet()
    }.toMap()
}

/** Compute dominators for a flowchart program. */
fun FlowchartProgram.computeDominators(): Map<RegularBlockLabel, Set<RegularBlockLabel>> {
    return computeDominators(this.successorMap, setOf(ENTRY_POINT_LABEL)) { set -> DataFlowSet(set) }
}

/** Compute postdominators for a flowchart program
 *  by computing dominators in inverse CFG. */
fun FlowchartProgram.computePostdominators(): Map<RegularBlockLabel, Set<RegularBlockLabel>> {
    return computeDominators(this.predecessorMap, this.exitPoints) { set -> DataFlowSet(set) }
}
/** Compute transitive successors for a flowchart program.
 *  Similar to postdominator analysis, except invert lattice so the meet operator is union instead of intersect:
 *  We care about successors from *any* path, not *all* paths.
 * */
fun FlowchartProgram.computeTransitiveSuccessors(): Map<RegularBlockLabel, Set<RegularBlockLabel>> {
    return computeDominators(this.predecessorMap, this.exitPoints) { set -> InvertedDataFlowSet(set) }
}
