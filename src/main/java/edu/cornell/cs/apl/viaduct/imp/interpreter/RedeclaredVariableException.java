package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

public class RedeclaredVariableException extends Exception {
  public RedeclaredVariableException(Variable variable) {
    super("Variable declared multiple times: " + variable);
  }
}
