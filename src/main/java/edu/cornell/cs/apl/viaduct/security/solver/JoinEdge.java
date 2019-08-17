package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.Lattice;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import org.jgrapht.graph.DefaultEdge;

/** Applies meet with a constant before passing on the incoming value. */
class JoinEdge<A extends Lattice<A>> extends DefaultEdge implements DataFlowEdge<A> {
  private final A elem;

  JoinEdge(A elem) {
    this.elem = elem;
  }

  public A getJoinConstant() {
    return this.elem;
  }

  @Override
  public A propagate(A other) {
    return this.elem.join(other);
  }
}
