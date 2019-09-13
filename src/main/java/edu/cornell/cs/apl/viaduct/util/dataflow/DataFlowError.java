package edu.cornell.cs.apl.viaduct.util.dataflow;

import java.util.Map;

/** Gives more information about the error when data flow analysis fails. */
public final class DataFlowError<A, NodeT extends DataFlowNode<A>, EdgeT extends DataFlowEdge<A>> {
  private final EdgeT unsatisfiableEdge;
  private final Map<NodeT, A> suboptimalAssignments;

  DataFlowError(EdgeT edge, Map<NodeT, A> suboptimalAssignments) {
    this.unsatisfiableEdge = edge;
    this.suboptimalAssignments = suboptimalAssignments;
  }

  /** Return the edge that caused the data flow analysis to fail. */
  public DataFlowEdge<A> getUnsatisfiableEdge() {
    return unsatisfiableEdge;
  }

  /**
   * Return a map that assigns a value to each node. This is the state the data flow analysis was in
   * just before it failed. The values are not guaranteed to (and often will not) be optimal, but
   * they are always an upper bound on the solution.
   */
  public Map<NodeT, A> getSuboptimalAssignments() {
    return suboptimalAssignments;
  }
}
