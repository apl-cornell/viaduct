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

private data class DataFlowSet<T>(val set: PersistentSet<T>): MeetSemiLattice<DataFlowSet<T>>{
    override fun meet(that: DataFlowSet<T>): DataFlowSet<T> {
        return DataFlowSet(this.set.intersect(that.set))
    }

    override fun lessThanOrEqualTo(that: DataFlowSet<T>): Boolean =
        this.set.containsAll(that.set)
}

private data class DominatorBlockNode(
    val label: RegularBlockLabel,
    val isEntryPoint: Boolean
): DataFlowNode<DataFlowSet<RegularBlockLabel>> {
    override fun transfer(input: DataFlowSet<RegularBlockLabel>): DataFlowSet<RegularBlockLabel> {
        // recall that the top element is all labels; if we don't special case entry points,
        // then the analysis will compute top for all nodes
        return if (isEntryPoint) {
            DataFlowSet(persistentSetOf(label))

        } else {
            DataFlowSet(input.set.add(label))
        }
    }
}

private fun computeDominators(
    successorMap: Map<RegularBlockLabel, Set<RegularBlockLabel>>,
    entryPoints: Set<RegularBlockLabel>
): Map<RegularBlockLabel, Set<RegularBlockLabel>> {
    val graph = DefaultDirectedGraph<DominatorBlockNode, IdentityEdge<DataFlowSet<RegularBlockLabel>>>(null, null, false)
    val nodeMap = successorMap.keys.associateWith { label ->
        DominatorBlockNode(label, entryPoints.contains(label))
    }

    for (kv in successorMap) {
        val src = nodeMap[kv.key]!!
        graph.addVertex(src)

        for (successor in kv.value) {
            val dst = nodeMap[successor]!!
            val edge = IdentityEdge<DataFlowSet<RegularBlockLabel>>()
            graph.addVertex(dst)
            graph.addEdge(src, dst, edge)
        }
    }

    return solveDataFlow(DataFlowSet(nodeMap.keys.toPersistentSet()), graph).map { kv ->
        kv.key.label to kv.value.set
    }.toMap()
}

fun FlowchartProgram.computeDominators(): Map<RegularBlockLabel, Set<RegularBlockLabel>> {
    return computeDominators(this.successorMap, setOf(ENTRY_POINT_LABEL))
}

fun FlowchartProgram.computePostdominators(): Map<RegularBlockLabel, Set<RegularBlockLabel>> {
    return computeDominators(this.predecessorMap, this.exitPoints)
}
