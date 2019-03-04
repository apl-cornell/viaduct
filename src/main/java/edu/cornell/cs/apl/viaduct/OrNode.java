package edu.cornell.cs.apl.viaduct;

/** boolean disjunction expression. */
public class OrNode implements BinaryExprNode {
  ExprNode lhs;
  ExprNode rhs;

  public OrNode(ExprNode lhs, ExprNode rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public ExprNode getLhs() {
    return this.lhs;
  }

  public ExprNode getRhs() {
    return this.rhs;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(|| " + this.lhs.toString() + " " + this.rhs.toString() + ")";
  }
}
