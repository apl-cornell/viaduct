package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.surface.Variable;

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
