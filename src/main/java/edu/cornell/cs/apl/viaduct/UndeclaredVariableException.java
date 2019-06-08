package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

// TODO: remove. There are two of these!

/** referenced or assigned variable was undeclared. */
public class UndeclaredVariableException extends RuntimeException {
  Variable var;

  public UndeclaredVariableException(Variable var) {
    super();
    this.var = var;
  }

  public Variable getVar() {
    return this.var;
  }
}
