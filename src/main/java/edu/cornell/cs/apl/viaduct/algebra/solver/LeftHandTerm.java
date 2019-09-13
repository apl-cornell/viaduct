package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;

/** Terms that can appear on the left-hand side of constraints. */
public interface LeftHandTerm<A> extends ConstraintTerm<A> {
  /**
   * Return an edge that captures the operation preformed by this term. In the constraint graph,
   * this will become an incoming edge of {@link #getNode()}.
   */
  DataFlowEdge<A> getInEdge();
}
