package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.values.IntegerValue;
import java.io.PrintStream;

/** Raised when an illegal array index is accessed during evaluation. */
public class ImpArrayOutOfBoundsError extends CompilationError {
  private final ArrayIndexingNode node;
  private final int arrayLength;
  private final int index;

  /**
   * Create an error description given the location that caused the error, as well as the dynamic
   * array bounds and the accessed index.
   *
   * @param node location in the source where the illegal access occurred
   * @param arrayLength dynamic size of the array
   * @param index illegal index the program tried to access
   */
  public ImpArrayOutOfBoundsError(ArrayIndexingNode node, int arrayLength, int index) {
    this.node = node;
    this.arrayLength = arrayLength;
    this.index = index;
  }

  @Override
  protected String getCategory() {
    return "Array Access Error";
  }

  @Override
  protected String getSource() {
    return node.getSourceLocation().getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.println("This code tried to access an invalid index:");

    output.println();
    node.getSourceLocation().showInSource(output);

    output.print("Index was ");
    IntegerValue.create(index).print(output);
    output.print(". Array had size ");
    IntegerValue.create(arrayLength).print(output);
    output.println(".");

    output.println();
  }
}
