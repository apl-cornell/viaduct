package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import java.io.PrintStream;

/**
 * Raised when downgrade nodes violate the non-malleable information flow control flow restriction.
 */
public class MalleableDowngradeError extends InformationFlowError {
  private final DowngradeNode node;

  public MalleableDowngradeError(DowngradeNode node) {
    this.node = node;
  }

  @Override
  protected String getCategory() {
    return "Malleable Downgrade";
  }

  @Override
  protected String getSource() {
    return node.getSourceLocation().getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.println("This downgrade node violates the non-malleability condition:");

    output.println();
    node.getSourceLocation().showInSource(output);
  }
}
