package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpType;
import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import java.io.PrintStream;

/** Thrown when the inferred type of an AST node does nat match its expected type. */
public class TypeMismatchError extends CompilationError {
  private final HasLocation node;
  private final ImpType actualType;
  private final ImpType expectedType;

  /**
   * Indicates that an AST node has an incorrect type.
   *
   * @param node node that has the incorrect type
   * @param actualType inferred type for the node
   * @param expectedType type the node should have
   */
  public TypeMismatchError(HasLocation node, ImpType actualType, ImpType expectedType) {
    this.node = node;
    this.actualType = actualType;
    this.expectedType = expectedType;
  }

  @Override
  protected String getCategory() {
    return "Type Mismatch";
  }

  @Override
  protected String getSource() {
    return node.getSourceLocation().getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.println("This term does not have the type I expect:");

    output.println();
    node.getSourceLocation().showInSource(output);

    output.println("It has type:");
    output.println();
    addIndentation(output);
    Printer.run(actualType, output);
    output.println();

    output.println();
    output.println("But it should have type:");
    output.println();
    addIndentation(output);
    Printer.run(expectedType, output);
    output.println();

    output.println();
  }
}
