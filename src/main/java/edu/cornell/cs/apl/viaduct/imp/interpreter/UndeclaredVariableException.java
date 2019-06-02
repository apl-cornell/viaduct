package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

public class UndeclaredVariableException extends Exception {
  public UndeclaredVariableException(Variable variable) {
    super("Variable accessed before it was declared: " + variable);
  }
}
