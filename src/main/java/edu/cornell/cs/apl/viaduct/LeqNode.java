package edu.cornell.cs.apl.viaduct;

/** less than or equal to comparison between arithmetic exprs.  */
public class LeqNode implements BinaryExprNode {
  ExprNode lhs;
  ExprNode rhs;

  public LeqNode(ExprNode lhs, ExprNode rhs) {
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
    return "(<= " + this.lhs.toString() + " " + this.rhs.toString() + ")";
  }
}
