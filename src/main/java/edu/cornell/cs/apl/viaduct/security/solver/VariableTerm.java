package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.BrouwerianLattice;

/** A variable for the solver to find a value for. */
public final class VariableTerm<A extends BrouwerianLattice<A>> implements ConstraintValue<A> {
  private final String id;
  private final String label;
  private final A bottom;

  VariableTerm(String id, A bottom) {
    this.id = id;
    this.label = id;
    this.bottom = bottom;
  }

  VariableTerm(String id, String label, A bottom) {
    this.id = id;
    this.label = label;
    this.bottom = bottom;
  }

  public String getId() {
    return this.id;
  }

  public String getLabel() {
    return this.label;
  }

  @Override
  public A initialize() {
    return this.bottom;
  }

  @Override
  public A transfer(A newValue) {
    return newValue;
  }

  @Override
  public String toString() {
    return this.label;
  }
}
