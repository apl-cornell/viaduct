package edu.cornell.cs.apl.viaduct.security.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.util.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;

/** Join of a constant element and a variable. */
@AutoValue
public abstract class ConstantJoinVariableTerm<A extends HeytingAlgebra<A>>
    implements RightHandTerm<A> {
  public static <A extends HeytingAlgebra<A>> ConstantJoinVariableTerm<A> create(
      A lhs, VariableTerm<A> rhs) {
    return new AutoValue_ConstantJoinVariableTerm<>(lhs, rhs);
  }

  protected abstract A getLhs();

  protected abstract VariableTerm<A> getRhs();

  @Override
  public final ConstraintValue<A> getNode() {
    return getRhs();
  }

  @Override
  public final DataFlowEdge<A> getOutEdge() {
    return new PseudocomplementEdge<>(getLhs());
  }
}
