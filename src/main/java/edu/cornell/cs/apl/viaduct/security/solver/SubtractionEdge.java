package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.CoHeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import org.jgrapht.graph.DefaultEdge;

/** Subtracts a constant from the incoming value before passing it on. */
class SubtractionEdge<A extends CoHeytingAlgebra<A>> extends DefaultEdge
    implements DataFlowEdge<A> {
  private final A rhs;

  SubtractionEdge(A rhs) {
    this.rhs = rhs;
  }

  @Override
  public A propagate(A lhs) {
    return lhs.subtract(rhs);
  }
}
