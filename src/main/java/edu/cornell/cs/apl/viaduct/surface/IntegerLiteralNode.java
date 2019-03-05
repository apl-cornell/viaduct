package edu.cornell.cs.apl.viaduct.surface;

import edu.cornell.cs.apl.viaduct.ExprVisitor;

/** Integer literal. */
public class IntegerLiteralNode extends LiteralNode<Integer> {
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
