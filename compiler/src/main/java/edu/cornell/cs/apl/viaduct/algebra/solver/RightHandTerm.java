package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;

/** Terms that can appear on the right-hand side of constraints. */
public interface RightHandTerm<A extends HeytingAlgebra<A>> extends ConstraintTerm<A> {
  /**
   * Return an edge that captures the operation preformed by this term. In the constraint graph,
   * this will become an outgoing edge of {@link #getNode()}.
   */
  DataFlowEdge<A> getOutEdge();
}
