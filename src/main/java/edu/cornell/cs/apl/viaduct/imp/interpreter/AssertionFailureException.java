package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;

class AssertionFailureException extends Exception {
  AssertionFailureException(ExpressionNode expr) {
    super("The following is asserted to be true, but is false: " + expr);
  }
}
