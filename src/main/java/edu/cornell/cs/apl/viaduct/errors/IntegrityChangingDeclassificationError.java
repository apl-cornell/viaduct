package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.io.PrintStream;

/** Raised when a declassify node modifies integrity. */
public class IntegrityChangingDeclassificationError extends InformationFlowError {
  private final DowngradeNode node;
  private final Label fromIntegrity;
  private final Label toIntegrity;

  /**
   * Construct an instance of the error.
   *
   * @param node declassify node that modifies integrity
   * @param from label of the expression being declassified
   * @param to output label
   */
  public IntegrityChangingDeclassificationError(DowngradeNode node, Label from, Label to) {
    this.node = node;
    this.fromIntegrity = from.integrity();
    this.toIntegrity = to.integrity();
  }

  @Override
  protected String getCategory() {
    return "Declassify Changes Integrity";
  }

  @Override
  protected String getSource() {
    return node.getSourceLocation().getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.println("This declassification expression modifies integrity:");

    output.println();
    node.getSourceLocation().showInSource(output);

    output.println("Original integrity of the expression:");
    output.println();
    addIndentation(output);
    Printer.run(fromIntegrity, output);
    output.println();

    output.println();
    output.println("Output integrity:");
    output.println();
    addIndentation(output);
    Printer.run(toIntegrity, output);
    output.println();

    output.println();
  }
}
