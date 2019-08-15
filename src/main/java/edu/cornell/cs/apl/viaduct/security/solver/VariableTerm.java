package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.BrouwerianLattice;

/** A variable for the solver to find a value for. */
public final class VariableTerm<A extends BrouwerianLattice<A>> implements ConstraintValue<A> {
  private final A bottom;

  VariableTerm(A bottom) {
    this.bottom = bottom;
  }

  @Override
  public A initialize() {
    return this.bottom;
  }

  @Override
  public A transfer(A newValue) {
    return newValue;
  }
}
