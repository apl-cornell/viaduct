package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Integer literal. */
public class IntegerLiteralNode
    extends LiteralNode<Integer> implements ImpValue {

  public IntegerLiteralNode(int value) {
    super(value);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(int " + this.getValue() + ")";
  }
}
