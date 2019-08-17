package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import org.jgrapht.graph.DefaultEdge;

/** Subtracts a constant from the incoming value before passing it on. */
class PseudocomplementEdge<A extends HeytingAlgebra<A>> extends DefaultEdge
    implements DataFlowEdge<A> {
  private final A elem;

  PseudocomplementEdge(A elem) {
    this.elem = elem;
  }

  public A getPseudocomplementedConstant() {
    return this.elem;
  }

  @Override
  public A propagate(A other) {
    return this.elem.relativePseudocomplement(other);
  }
}
