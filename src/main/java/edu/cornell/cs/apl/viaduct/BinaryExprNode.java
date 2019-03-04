package edu.cornell.cs.apl.viaduct;

/** binary operation expression. */
public abstract class BinaryExprNode implements ExprNode {
  ExprNode lhs;
  ExprNode rhs;

  public BinaryExprNode(ExprNode lhs, ExprNode rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  ExprNode getLhs() {
    return this.lhs;
  }

  ExprNode getRhs() {
    return this.rhs;
  }
}
