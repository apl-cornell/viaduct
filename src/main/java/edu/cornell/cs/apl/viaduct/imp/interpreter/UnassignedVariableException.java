package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

class UnassignedVariableException extends Exception {
  UnassignedVariableException(Variable variable) {
    super("Variable read before it was assigned a value: " + variable);
  }
}
