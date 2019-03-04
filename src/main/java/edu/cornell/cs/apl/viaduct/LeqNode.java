package edu.cornell.cs.apl.viaduct;

/** less than or equal to comparison between arithmetic exprs. */
public class LeqNode extends BinaryExprNode {
  public LeqNode(ExprNode lhs, ExprNode rhs) {
    super(lhs, rhs);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(<= " + this.lhs.toString() + " " + this.rhs.toString() + ")";
  }
}
