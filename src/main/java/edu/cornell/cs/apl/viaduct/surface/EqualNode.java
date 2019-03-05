package edu.cornell.cs.apl.viaduct.surface;

import edu.cornell.cs.apl.viaduct.ExprVisitor;

/** Check if two expressions are equal. */
public class EqualNode extends BinaryExpressionNode {
  public EqualNode(ExpressionNode lhs, ExpressionNode rhs) {
    super(lhs, rhs);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(== " + this.getLhs().toString() + " " + this.getRhs().toString() + ")";
  }
}
