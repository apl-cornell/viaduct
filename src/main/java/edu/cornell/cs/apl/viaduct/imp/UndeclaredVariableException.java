package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.CompilationException;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

// TODO: SpotBugs says these should not be named ...Exception.
//   Suppressed the warning for now. Maybe we want to rename these calsses.

public class UndeclaredVariableException extends CompilationException {
  public UndeclaredVariableException(Variable variable) {
    super("Variable accessed before it was declared: " + variable);
  }
}
