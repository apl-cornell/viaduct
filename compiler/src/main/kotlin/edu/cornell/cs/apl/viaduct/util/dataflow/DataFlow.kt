package edu.cornell.cs.apl.viaduct.util.dataflow

import edu.cornell.cs.apl.viaduct.algebra.MeetSemiLattice
import edu.cornell.cs.apl.viaduct.util.UniqueQueue
import java.util.Queue
import org.jgrapht.Graph
import org.jgrapht.Graphs
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector
import org.jgrapht.traverse.TopologicalOrderIterator

/**
 * Run data flow analysis on the given graph and return the computed solution for each node. The
 * solution for a node is the return value of the last call to [DataFlowNode.transfer].
 *
 * @param top greatest element of `A`
 * @param graph data flow graph to run the analysis on
 */
fun <A : MeetSemiLattice<A>, NodeT : DataFlowNode<A>, EdgeT : DataFlowEdge<A>> solveDataFlow(
    top: A,
    graph: Graph<NodeT, EdgeT>
): Map<NodeT, A> {
    return DataFlow(top, graph).run()
}

/** A solver that computes the greatest solution to a set of data flow equations. */
private class DataFlow<A : MeetSemiLattice<A>, NodeT : DataFlowNode<A>, EdgeT : DataFlowEdge<A>> constructor(
    private val top: A,
    private val graph: Graph<NodeT, EdgeT>
) {
    private val nodeOutValues = mutableMapOf<NodeT, A>()

    private val edgeOutValues = mutableMapOf<EdgeT, A>()

    fun run(): Map<NodeT, A> {
        // Initialize nodes
        for (node in graph.vertexSet()) {
            setNodeOutValue(node, node.transfer(top))
        }

        // Break into strongly connected components for efficiency
        val stronglyConnectedComponents = KosarajuStrongConnectivityInspector(graph).condensation

        // Solve components in topological order (i.e. visiting dependencies before dependents)
        TopologicalOrderIterator(stronglyConnectedComponents).forEach(::solveComponent)

        return nodeOutValues
    }

    /**
     * Update the out value associated with a node, and the out values associated with all its
     * outgoing edges.
     *
     * @param node the node to update
     * @param value the new out value
     * @return `true` if the value is changed
     */
    private fun setNodeOutValue(node: NodeT, value: A): Boolean {
        val oldValue = nodeOutValues[node]
        if (value == oldValue) {
            return false
        }
        nodeOutValues[node] = value
        for (edge in graph.outgoingEdgesOf(node)) {
            edgeOutValues[edge] = edge.propagate(value)
        }
        return true
    }

    /**
     * Find a solution for all nodes in a strongly connected component.
     *
     * Assumes that all other components that have edges into this component have already been
     * solved.
     */
    private fun solveComponent(component: Graph<NodeT, EdgeT>) {
        val workList: Queue<NodeT> = UniqueQueue(component.vertexSet())
        while (!workList.isEmpty()) {
            val node = workList.remove()

            // Find incoming edges, which represent data dependencies.
            // NOTE: we have to use the global graph not the component!
            val incomingEdges = graph.incomingEdgesOf(node)

            // Compute the incoming value for the current node.
            var inValue = top
            for (inEdge in incomingEdges) {
                val inEdgeValue = edgeOutValues[inEdge]!!
                inValue = inValue.meet(inEdgeValue)
            }

            // Derive the out value from the in value.
            val outValue = node.transfer(inValue)
            if (setNodeOutValue(node, outValue)) {
                // NOTE: unlike before, we use the local graph when computing successors.
                workList.addAll(Graphs.successorListOf(component, node))
            }
        }
    }
}
