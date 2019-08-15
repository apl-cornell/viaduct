package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.Lattice;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import org.jgrapht.graph.DefaultEdge;

/** Applies meet with a constant before passing on the incoming value. */
class JoinEdge<A extends Lattice<A>> extends DefaultEdge implements DataFlowEdge<A> {
  private final A lhs;

  JoinEdge(A lhs) {
    this.lhs = lhs;
  }

  public A getJoinConstant() {
    return this.lhs;
  }

  @Override
  public A propagate(A rhs) {
    return lhs.join(rhs);
  }
}
