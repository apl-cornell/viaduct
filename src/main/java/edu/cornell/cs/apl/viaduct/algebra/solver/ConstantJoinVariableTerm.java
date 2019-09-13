package edu.cornell.cs.apl.viaduct.algebra.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;

/** Join of a constant element and a variable. */
@AutoValue
abstract class ConstantJoinVariableTerm<A extends HeytingAlgebra<A>> implements RightHandTerm<A> {
  static <A extends HeytingAlgebra<A>> ConstantJoinVariableTerm<A> create(
      A lhs, VariableTerm<A> rhs) {
    return new AutoValue_ConstantJoinVariableTerm<>(lhs, rhs);
  }

  protected abstract A getLhs();

  protected abstract VariableTerm<A> getRhs();

  @Override
  public final AtomicTerm<A> getNode() {
    return getRhs();
  }

  @Override
  public final DataFlowEdge<A> getOutEdge() {
    return new JoinEdge<>(getLhs());
  }
}
