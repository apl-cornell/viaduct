package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Integer add. */
public class PlusNode extends BinaryExpressionNode {
  public PlusNode(ExpressionNode lhs, ExpressionNode rhs) {
    super(lhs, rhs);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(+ " + this.getLhs().toString() + " " + this.getRhs().toString() + ")";
  }
}
