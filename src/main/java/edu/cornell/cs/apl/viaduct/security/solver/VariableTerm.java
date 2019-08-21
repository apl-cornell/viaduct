package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.util.HeytingAlgebra;

/** A variable for the solver to find a value for. */
public final class VariableTerm<A extends HeytingAlgebra<A>> extends ConstraintValue<A> {
  private final String label;
  private final A top;

  VariableTerm(String id, A top) {
    super(id);
    this.label = id;
    this.top = top;
  }

  VariableTerm(String id, String label, A top) {
    super(id);
    this.label = label;
    this.top = top;
  }

  @Override
  public String getId() {
    return this.id;
  }

  public String getLabel() {
    return this.label;
  }

  @Override
  public A initialize() {
    return this.top;
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
