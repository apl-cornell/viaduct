package edu.cornell.cs.apl.viaduct;

/** boolean literal (true/false). */
public class BoolLiteralNode implements ExprNode {
  boolean val;

  public BoolLiteralNode(boolean val) {
    this.val = val;
  }

  public boolean getVal() {
    return this.val;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(bool " + Boolean.toString(this.val) + ")";
  }
}
