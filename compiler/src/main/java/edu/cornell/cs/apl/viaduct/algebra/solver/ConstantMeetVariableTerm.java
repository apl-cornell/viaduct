package edu.cornell.cs.apl.viaduct.algebra.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;
import java.util.Map;

/** Meet of a constant element and a variable. */
@AutoValue
abstract class ConstantMeetVariableTerm<A extends HeytingAlgebra<A>> implements LeftHandTerm<A> {
  static <A extends HeytingAlgebra<A>> ConstantMeetVariableTerm<A> create(
      A lhs, VariableTerm<A> rhs) {
    return new AutoValue_ConstantMeetVariableTerm<>(lhs, rhs);
  }

  protected abstract A getLhs();

  protected abstract VariableTerm<A> getRhs();

  @Override
  public A getValue(Map<VariableTerm<A>, A> solution) {
    return getLhs().meet(getRhs().getValue(solution));
  }

  @Override
  public final AtomicTerm<A> getNode() {
    return getRhs();
  }

  @Override
  public final DataFlowEdge<A> getInEdge() {
    return new ImplyEdge<>(getLhs());
  }
}
