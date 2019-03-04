package edu.cornell.cs.apl.viaduct;

/** adds two expressions. */
public class PlusNode extends BinaryExprNode {
  public PlusNode(ExprNode lhs, ExprNode rhs) {
    super(lhs, rhs);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(+ " + this.lhs.toString() + " " + this.rhs.toString() + ")";
  }
}
