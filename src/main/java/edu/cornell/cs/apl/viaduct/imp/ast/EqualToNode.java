package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Check if two expressions are equal. */
public class EqualToNode extends BinaryExpressionNode {
  public EqualToNode(ExpressionNode lhs, ExpressionNode rhs) {
    super(lhs, rhs);
  }

  @Override
  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String getOpStr() {
    return "==";
  }
}
