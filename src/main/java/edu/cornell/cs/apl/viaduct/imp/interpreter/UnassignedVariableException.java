package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.CompilationException;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

class UnassignedVariableException extends CompilationException {
  UnassignedVariableException(Variable variable) {
    super("Variable read before it was assigned a value: " + variable);
  }
}
