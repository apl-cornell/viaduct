package edu.cornell.cs.apl.viaduct;

/** boolean AND expression. */
public class AndNode extends BinaryExprNode {
  public AndNode(ExprNode lhs, ExprNode rhs) {
    super(lhs, rhs);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(&& " + this.lhs.toString() + " " + this.rhs.toString() + ")";
  }
}
