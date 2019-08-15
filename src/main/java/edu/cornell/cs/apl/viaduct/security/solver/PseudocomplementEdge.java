package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.BrouwerianLattice;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import org.jgrapht.graph.DefaultEdge;

/** Subtracts a constant from the incoming value before passing it on. */
class PseudocomplementEdge<A extends BrouwerianLattice<A>> extends DefaultEdge
    implements DataFlowEdge<A> {
  private final A rhs;

  PseudocomplementEdge(A rhs) {
    this.rhs = rhs;
  }

  @Override
  public A propagate(A lhs) {
    return lhs.relativePseudocomplement(rhs);
  }
}
