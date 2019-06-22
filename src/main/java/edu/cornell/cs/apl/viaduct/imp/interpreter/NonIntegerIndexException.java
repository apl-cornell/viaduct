package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

class NonIntegerIndexException extends Exception {
  NonIntegerIndexException(Variable var, ImpValue val) {
    super(String.format("Index for arrray %s is not an integer: %s",
        var.toString(), val.toString()));
  }
}
