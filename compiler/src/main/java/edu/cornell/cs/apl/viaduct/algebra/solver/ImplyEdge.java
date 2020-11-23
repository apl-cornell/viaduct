package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import org.jgrapht.graph.DefaultEdge;

/** Maps incoming values {@code input} to {@code c.imply(input)}. */
final class ImplyEdge<A extends HeytingAlgebra<A>> extends DefaultEdge implements DataFlowEdge<A> {
  private final A antecedent;

  ImplyEdge(A antecedent) {
    this.antecedent = antecedent;
  }

  @Override
  public A propagate(A input) {
    return antecedent.imply(input);
  }

  @Override
  public String toString() {
    return antecedent + " → _";
  }
}
