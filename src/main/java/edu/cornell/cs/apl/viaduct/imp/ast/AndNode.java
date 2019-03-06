package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Boolean AND expression. */
public class AndNode extends BinaryExpressionNode {
  public AndNode(ExpressionNode lhs, ExpressionNode rhs) {
    super(lhs, rhs);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(&& " + this.getLhs().toString() + " " + this.getRhs().toString() + ")";
  }
}
