package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.CompilationException;

public class UnsatisfiableConstraintException extends CompilationException {
  // TODO: better error location reporting
  public UnsatisfiableConstraintException(RightHandTerm location) {
    super("Unsatisfiable constraint for term: " + location);
  }
}
