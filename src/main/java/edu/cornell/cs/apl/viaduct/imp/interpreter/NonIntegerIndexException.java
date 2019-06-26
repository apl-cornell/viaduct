package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.CompilationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

class NonIntegerIndexException extends CompilationException {
  NonIntegerIndexException(Variable array, ImpValue index) {
    super(String.format("Index for array %s is not an integer: %s", array, index));
  }
}
