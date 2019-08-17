package edu.cornell.cs.apl.viaduct.util.dataflow;

import edu.cornell.cs.apl.viaduct.util.Lattice;
import edu.cornell.cs.apl.viaduct.util.UniqueQueue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class DataFlow<
    A extends Lattice<A>,
    T extends Throwable,
    NodeT extends DataFlowNode<A, T>,
    EdgeT extends DataFlowEdge<A>> {

  public enum DataflowDirection { UP, DOWN }

  private final A init;

  private final Graph<NodeT, EdgeT> graph;

  private final Map<NodeT, A> nodeOutValues = new HashMap<>();
  private final Map<EdgeT, A> edgeOutValues = new HashMap<>();

  private final DataflowDirection direction;

  protected DataFlow(A init, Graph<NodeT, EdgeT> graph, DataflowDirection dir) {
    this.init = init;
    this.graph = graph;
    this.direction = dir;
  }

  /**
   * Run data flow analysis on the given graph and return the computed solution for each node.
   *
   * @param init initial value {@code A}
   * @param graph data flow graph to run the analysis on
   * @param dir direction of analysis, changes whether to use joins or meets
   */
  public static <
          A extends Lattice<A>,
          T extends Throwable,
          NodeT extends DataFlowNode<A, T>,
          EdgeT extends DataFlowEdge<A>>
      Map<NodeT, A> solve(A init, Graph<NodeT, EdgeT> graph, DataflowDirection dir) throws T {
    return new DataFlow<>(init, graph, dir).run();
  }

  private Map<NodeT, A> run() throws T {
    // Initialize nodes
    for (NodeT node : graph.vertexSet()) {
      setNodeOutValue(node, node.initialize());
    }

    // Break into strongly connected components for efficiency
    final Graph<Graph<NodeT, EdgeT>, DefaultEdge> stronglyConnectedComponents =
        new KosarajuStrongConnectivityInspector<>(graph).getCondensation();

    // Solve components in topological order (i.e. visiting dependencies before dependents)
    Iterator<Graph<NodeT, EdgeT>> topologicalIterator =
        new TopologicalOrderIterator<>(stronglyConnectedComponents);
    while (topologicalIterator.hasNext()) {
      solveComponent(topologicalIterator.next());
    }

    return nodeOutValues;
  }

  /**
   * Update the out value associated with a node, and the out values associated with all its
   * outgoing edges.
   *
   * @param node the node to update
   * @param value the new out value
   * @return {@code true} if the value is changed
   */
  private boolean setNodeOutValue(NodeT node, A value) {
    final A oldValue = nodeOutValues.get(node);

    if (value.equals(oldValue)) {
      return false;
    }

    nodeOutValues.put(node, value);
    for (EdgeT edge : graph.outgoingEdgesOf(node)) {
      edgeOutValues.put(edge, edge.propagate(value));
    }
    return true;
  }

  /**
   * Find a solution for all nodes in a strongly connected component.
   *
   * <p>Assumes that all other components that have edges into this component have already been
   * solved.
   */
  private void solveComponent(Graph<NodeT, EdgeT> component) throws T {
    final Queue<NodeT> workList = new UniqueQueue<>(component.vertexSet());

    while (!workList.isEmpty()) {
      final NodeT node = workList.remove();

      // Find incoming edges, which represent data dependencies.
      // NOTE: using the original graph not the component!
      final Set<EdgeT> incomingEdges = graph.incomingEdgesOf(node);

      // Compute in value for the current node.
      A inValue = init;
      for (EdgeT inEdge : incomingEdges) {
        A inEdgeValue = edgeOutValues.get(inEdge);

        switch (this.direction) {
          case UP:
            inValue = inValue.join(inEdgeValue);
            break;

          case DOWN:
            inValue = inValue.meet(inEdgeValue);
            break;

          default:
            break;
        }
      }

      // Derive out value from the in value.
      final A outValue = node.transfer(inValue);

      if (setNodeOutValue(node, outValue)) {
        // NOTE: unlike before, we use the local graph when computing successors.
        workList.addAll(Graphs.successorListOf(component, node));
      }
    }
  }
}
