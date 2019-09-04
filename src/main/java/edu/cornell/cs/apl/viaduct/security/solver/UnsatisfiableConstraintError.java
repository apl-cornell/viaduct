package edu.cornell.cs.apl.viaduct.security.solver;

public class UnsatisfiableConstraintError extends Error {
  /** Constructor. */
  // TODO: better error location reporting
  public UnsatisfiableConstraintError(ConstraintValue val, Object oldVal, Object newVal) {
    super(
        String.format(
            "Unsatisfied constraint node %s has value %s, expected %s",
            val.getId(), oldVal, newVal));
  }
}
