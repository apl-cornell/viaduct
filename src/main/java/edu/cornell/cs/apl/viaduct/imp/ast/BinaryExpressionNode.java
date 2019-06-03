package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import java.util.Objects;

/** Superclass of binary operation expressions. */
public final class BinaryExpressionNode extends ExpressionNode {
  private final ExpressionNode lhs;
  private final ExpressionNode rhs;
  private final BinaryOperator operator;

  private BinaryExpressionNode(ExpressionNode lhs, BinaryOperator operator, ExpressionNode rhs) {
    this.lhs = Objects.requireNonNull(lhs);
    this.rhs = Objects.requireNonNull(rhs);
    this.operator = Objects.requireNonNull(operator);
  }

  /** Create a binary expression given the operator and its two arguments. */
  public static BinaryExpressionNode create(
      ExpressionNode lhs, BinaryOperator operator, ExpressionNode rhs) {
    return new BinaryExpressionNode(lhs, operator, rhs);
  }

  public ExpressionNode getLhs() {
    return this.lhs;
  }

  public ExpressionNode getRhs() {
    return this.rhs;
  }

  public BinaryOperator getOperator() {
    return this.operator;
  }

  @Override
  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof BinaryExpressionNode)) {
      return false;
    }

    final BinaryExpressionNode that = (BinaryExpressionNode) other;
    return Objects.equals(this.lhs, that.lhs)
        && Objects.equals(this.rhs, that.rhs)
        && Objects.equals(this.operator, that.operator);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.lhs, this.rhs, this.operator);
  }

  @Override
  public String toString() {
    return String.format(
        "(%s %s %s)", this.operator.toString(), this.lhs.toString(), this.rhs.toString());
  }
}
