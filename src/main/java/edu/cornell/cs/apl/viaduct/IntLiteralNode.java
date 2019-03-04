package edu.cornell.cs.apl.viaduct;

/** integer literals. */
public class IntLiteralNode implements ExprNode {
  int val;

  public IntLiteralNode(int val) {
    this.val = val;
  }

  public int getVal() {
    return this.val;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(int " + Integer.toString(this.val) + ")";
  }
}
