package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpType;
import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;

public class TypeMismatchError extends CompilationError {
  private final HasLocation node;
  private final ImpType actualType;
  private final ImpType expectedType;

  /**
   * Captures the information that a node has an incorrect type.
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
}
