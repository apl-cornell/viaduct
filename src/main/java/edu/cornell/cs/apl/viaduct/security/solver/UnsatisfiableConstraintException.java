package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.CompilationException;

public class UnsatisfiableConstraintException extends CompilationException {
  // TODO: better error location reporting
  public UnsatisfiableConstraintException(ConstraintValue val, Object oldVal, Object newVal) {
    super(String.format("Unsatisfied constraint node %s has value %s, expected %s",
            val.getId(), oldVal, newVal));
  }
}
