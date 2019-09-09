package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;

/** Raised when an assertion in Imp source code fails during evaluation. */
public final class AssertionFailureError extends CompilationError {
  private final HasLocation assertion;

  public AssertionFailureError(AssertNode assertion) {
    this.assertion = assertion;
  }

  @Override
  protected String getCategory() {
    return "Assertion Failure";
  }

  @Override
  protected String getSource() {
    return assertion.getSourceLocation().getSourcePath();
  }
}
