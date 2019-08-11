package edu.cornell.cs.apl.viaduct.security.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.util.Lattice;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;

/** Meet of a constant element and a variable. */
@AutoValue
public abstract class ConstantMeetVariableTerm<A extends Lattice<A>> implements LeftHandTerm<A> {
  public static <A extends Lattice<A>> ConstantMeetVariableTerm<A> create(
      A lhs, ConstraintSystem<A>.VariableTerm rhs) {
    return new AutoValue_ConstantMeetVariableTerm<>(lhs, rhs);
  }

  protected abstract A getLhs();

  protected abstract ConstraintSystem<A>.VariableTerm getRhs();

  @Override
  public final ConstraintValue<A> getNode() {
    return getRhs();
  }

  @Override
  public final DataFlowEdge<A> getOutEdge() {
    return new MeetEdge<>(getLhs());
  }
}
