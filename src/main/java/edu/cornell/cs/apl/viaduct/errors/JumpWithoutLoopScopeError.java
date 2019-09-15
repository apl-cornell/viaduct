package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange;
import java.io.PrintStream;

/**
 * Thrown when a {@code break}, {@code continue}, or a similar statements occurs outside the scope
 * of a loop.
 */
public class JumpWithoutLoopScopeError extends CompilationError {
  private final SourceRange location;

  public JumpWithoutLoopScopeError(HasLocation node) {
    this.location = node.getSourceLocation();
  }

  @Override
  protected String getCategory() {
    return "Control Error";
  }

  @Override
  protected String getSource() {
    return location.getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.print("This statement is only valid inside a loop:");

    output.println();
    location.showInSource(output);
  }
}
