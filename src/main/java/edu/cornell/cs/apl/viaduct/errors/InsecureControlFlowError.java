package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.io.PrintStream;

/** Thrown when the control flow influences data in a way that violates security. */
public class InsecureControlFlowError extends InformationFlowError {
  private final HasLocation node;
  private final Label nodeLabel;
  private final Label pc;

  /**
   * Indicates that the program counter label affects the given node in a way that violates
   * security.
   *
   * @param node AST node influenced by control flow
   * @param nodeLabel security label of node
   * @param pc label assigned to control flow
   */
  public InsecureControlFlowError(HasLocation node, Label nodeLabel, Label pc) {
    this.node = node;
    this.nodeLabel = nodeLabel;
    this.pc = pc;
  }

  @Override
  protected String getCategory() {
    return "Insecure Control Flow";
  }

  @Override
  protected String getSource() {
    return node.getSourceLocation().getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    // TODO:
  }
}
