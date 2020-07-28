package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.algebra.Lattice;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import org.jgrapht.graph.DefaultEdge;

/** Joins the incoming value with a constant before passing it on. */
final class JoinEdge<A extends Lattice<A>> extends DefaultEdge implements DataFlowEdge<A> {
  private final A constant;

  JoinEdge(A constant) {
    this.constant = constant;
  }

  @Override
  public A propagate(A in) {
    return constant.join(in);
  }

  @Override
  public String toString() {
    return constant + " ∨ _";
  }
}
