package edu.cornell.cs.apl.viaduct.imp.ast;

/**
 * Superclass of binary operation expressions.
 *
 * <p>Specific operations (like addition or boolean AND) should inherit from this class.
 */
public abstract class BinaryExpressionNode extends ExpressionNode {
  private final ExpressionNode lhs;
  private final ExpressionNode rhs;

  public BinaryExpressionNode(ExpressionNode lhs, ExpressionNode rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public ExpressionNode getLhs() {
    return this.lhs;
  }

  public ExpressionNode getRhs() {
    return this.rhs;
  }
}
