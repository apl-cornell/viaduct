package edu.cornell.cs.apl.viaduct;

/** check if two integers are equal. */
public class EqualNode extends BinaryExprNode {
  public EqualNode(ExprNode lhs, ExprNode rhs) {
    super(lhs, rhs);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(== " + this.lhs.toString() + " " + this.rhs.toString() + ")";
  }
}
