package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.io.PrintStream;

/** Raised when an endorse node modifies confidentiality. */
public class ConfidentialityChangingEndorsementError extends InformationFlowError {
  private final DowngradeNode node;
  private final Label fromConfidentiality;
  private final Label toConfidentiality;

  /**
   * Construct an instance of the error.
   *
   * @param node endorse node that modifies integrity
   * @param from label of the expression being endorsed
   * @param to output label
   */
  public ConfidentialityChangingEndorsementError(DowngradeNode node, Label from, Label to) {
    this.node = node;
    this.fromConfidentiality = from.confidentiality();
    this.toConfidentiality = to.confidentiality();
  }

  @Override
  protected String getCategory() {
    return "Endorse Changes Confidentiality";
  }

  @Override
  protected String getSource() {
    return node.getSourceLocation().getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.println("This endorsement expression modifies confidentiality:");

    output.println();
    node.getSourceLocation().showInSource(output);

    output.println("Original confidentiality of the expression:");
    output.println();
    addIndentation(output);
    Printer.run(fromConfidentiality, output);
    output.println();

    output.println();
    output.println("Output confidentiality:");
    output.println();
    addIndentation(output);
    Printer.run(toConfidentiality, output);
    output.println();

    output.println();
  }
}
