package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

class ImpArrayOutOfBoundsException extends Exception {
  ImpArrayOutOfBoundsException(Variable var, int index) {
    super(String.format("Array %s out of bounds index at %d",
        var.toString(), index));
  }
}
