package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;

/** Superclass of binary operation expressions. */
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

  public abstract String getOpStr();

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other.getClass().equals(this.getClass())) {
      BinaryExpressionNode otherBinop = (BinaryExpressionNode) other;
      return otherBinop.lhs.equals(this.lhs) && otherBinop.rhs.equals(this.rhs);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(getOpStr(), this.lhs, this.rhs);
  }

  @Override
  public String toString() {
    return String.format("(%s %s %s)", getOpStr(), this.lhs.toString(), this.rhs.toString());
  }
}
