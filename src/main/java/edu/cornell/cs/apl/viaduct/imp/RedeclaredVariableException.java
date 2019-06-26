package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.CompilationException;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

public class RedeclaredVariableException extends CompilationException {
  public RedeclaredVariableException(Variable variable) {
    super("Variable declared multiple times: " + variable);
  }
}
