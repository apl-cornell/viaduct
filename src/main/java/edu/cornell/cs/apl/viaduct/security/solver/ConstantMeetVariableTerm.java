package edu.cornell.cs.apl.viaduct.security.solver;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.util.BrouwerianLattice;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;

/** Meet of a constant element and a variable. */
@AutoValue
public abstract class ConstantMeetVariableTerm<A extends BrouwerianLattice<A>>
    implements LeftHandTerm<A>
{
  public static <A extends BrouwerianLattice<A>> ConstantMeetVariableTerm<A> create(
      A lhs, VariableTerm<A> rhs) {
    return new AutoValue_ConstantMeetVariableTerm<>(lhs, rhs);
  }

  protected abstract A getLhs();

  protected abstract VariableTerm<A> getRhs();

  @Override
  public final ConstraintValue<A> getNode() {
    return getRhs();
  }

  @Override
  public final DataFlowEdge<A> getInEdge() {
    return new PseudocomplementEdge<>(getLhs());
  }
}
