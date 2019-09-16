package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.io.PrintStream;

/** Thrown when data from an AST node might flow to place that would violate security. */
public class InsecureDataFlowError extends InformationFlowError {
  private final HasLocation node;
  private final Label nodeLabel;
  private final Label to;

  /**
   * Indicates that the output value of an AST node flows to a location which violates security.
   *
   * @param node AST node whose output violates security
   * @param nodeLabel security label of node's output
   * @param to security label of the destination of node's output
   */
  public InsecureDataFlowError(HasLocation node, Label nodeLabel, Label to) {
    this.node = node;
    this.nodeLabel = nodeLabel;
    this.to = to;
  }

  @Override
  protected String getCategory() {
    return "Insecure Data Flow";
  }

  @Override
  protected String getSource() {
    return node.getSourceLocation().getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    if (!nodeLabel.confidentiality().flowsTo(to.confidentiality())) {
      // Confidentiality is the problem
      output.println("This term is flowing to a place that does not have enough confidentiality:");

      output.println();
      node.getSourceLocation().showInSource(output);

      output.println("The term's confidentiality label is:");
      output.println();
      addIndentation(output);
      Printer.run(nodeLabel.confidentiality(), output);
      output.println();

      output.println();
      output.println("But it is going to a place that only guarantees:");
      output.println();
      addIndentation(output);
      Printer.run(to.confidentiality(), output);
      output.println();
    } else {
      // Integrity is the problem
      assert !nodeLabel.integrity().flowsTo(to.integrity());

      output.println("This term does not have enough integrity:");

      output.println();
      node.getSourceLocation().showInSource(output);

      output.println("Its integrity label is:");
      output.println();
      addIndentation(output);
      Printer.run(nodeLabel.integrity(), output);
      output.println();

      output.println();
      output.println("But it needs to be at least:");
      output.println();
      addIndentation(output);
      Printer.run(to.integrity(), output);
      output.println();
    }

    output.println();
  }
}
