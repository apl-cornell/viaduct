package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.CompilationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;

class AssertionFailureException extends CompilationException {
  AssertionFailureException(ExpressionNode expr) {
    super("Assertion failed: " + PrintVisitor.run(expr));
  }
}
