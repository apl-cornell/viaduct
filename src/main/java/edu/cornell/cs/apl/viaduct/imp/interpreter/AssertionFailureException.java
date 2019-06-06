package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;

class AssertionFailureException extends Exception {
  AssertionFailureException(ExpressionNode expr) {
    super("Assertion failed: " + new PrintVisitor().run(expr));
  }
}
