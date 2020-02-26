package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
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

    if (!pc.confidentiality().flowsTo(nodeLabel.confidentiality())) {
      // Confidentiality is the problem
      output.println("Execution of this term might leak information encoded in the control flow:");

      output.println();
      node.getSourceLocation().showInSource(output);

      output.println("Confidentiality label on control flow is:");
      output.println();
      addIndentation(output);
      Printer.run(pc.confidentiality(), output);
      output.println();

      output.println();
      output.println("But the term only guarantees:");
      output.println();
      addIndentation(output);
      Printer.run(nodeLabel.confidentiality(), output);
      output.println();
    } else {
      // Integrity is the problem
      assert !pc.integrity().flowsTo(nodeLabel.integrity());

      output.println("The control flow does not have enough integrity for this term:");

      output.println();
      node.getSourceLocation().showInSource(output);

      output.println("Integrity label on control flow is:");
      output.println();
      addIndentation(output);
      Printer.run(pc.integrity(), output);
      output.println();

      output.println();
      output.println("But it needs to be at least:");
      output.println();
      addIndentation(output);
      Printer.run(nodeLabel.integrity(), output);
      output.println();
    }

    output.println();
  }
}
