package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.values.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import java.io.PrintStream;

/** Raised when an assertion in Imp source code fails during evaluation. */
public final class AssertionFailureError extends CompilationError {
  private final AssertNode assertion;

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

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.print("This assertion evaluated to ");
    Printer.run(BooleanValue.create(false), output);
    output.println(":");

    output.println();
    assertion.getSourceLocation().showInSource(output);
  }
}
