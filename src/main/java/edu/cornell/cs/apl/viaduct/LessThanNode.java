package edu.cornell.cs.apl.viaduct;

/** less than comparison between arithmetic exprs. */
public class LessThanNode extends BinaryExprNode {
  public LessThanNode(ExprNode lhs, ExprNode rhs) {
    super(lhs, rhs);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(< " + this.lhs.toString() + " " + this.rhs.toString() + ")";
  }
}
