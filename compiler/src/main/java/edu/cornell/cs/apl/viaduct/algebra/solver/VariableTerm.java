package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import java.util.Map;

/**
 * A variable for the solver to find a value for.
 *
 * @see ConstraintSystem#addNewVariable(Object) for generating instances.
 */
public final class VariableTerm<A extends HeytingAlgebra<A>> extends AtomicTerm<A> {
  private final Object label;

  /**
   * Create a fresh variable.
   *
   * @param label an arbitrary object to use as a label (useful for debugging)
   */
  VariableTerm(Object label) {
    this.label = label;
  }

  @Override
  public A getValue(Map<VariableTerm<A>, A> solution) {
    return solution.get(this);
  }

  @Override
  public LeftHandTerm<A> meet(A that) {
    return ConstantMeetVariableTerm.create(that, this);
  }

  @Override
  public RightHandTerm<A> join(A that) {
    return ConstantJoinVariableTerm.create(that, this);
  }

  @Override
  public A transfer(A input) {
    return input;
  }

  @Override
  public String toString() {
    return label.toString();
  }
}
