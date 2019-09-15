package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.io.PrintStream;

/** Thrown when the inferred label of an AST node does nat match its annotated label. */
public class LabelMismatchError extends InformationFlowError {
  private final HasLocation node;
  private final Label actualLabel;
  private final Label expectedLabel;

  /**
   * Indicates that an AST node has the incorrect label.
   *
   * @param node node that has the incorrect label
   * @param actualLabel inferred label for the node
   * @param expectedLabel annotated label for the node
   */
  public LabelMismatchError(HasLocation node, Label actualLabel, Label expectedLabel) {
    this.node = node;
    this.actualLabel = actualLabel;
    this.expectedLabel = expectedLabel;
  }

  @Override
  protected String getCategory() {
    return "Information Flow Label Mismatch";
  }

  @Override
  protected String getSource() {
    return node.getSourceLocation().getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.println("This term does not have the label I expect:");

    output.println();
    node.getSourceLocation().showInSource(output);

    output.println("I inferred its label as:");
    output.println();
    addIndentation(output);
    Printer.run(actualLabel, output);
    output.println();

    output.println();
    output.println("But its label should be:");
    output.println();
    addIndentation(output);
    Printer.run(expectedLabel, output);
    output.println();

    output.println();
  }
}
