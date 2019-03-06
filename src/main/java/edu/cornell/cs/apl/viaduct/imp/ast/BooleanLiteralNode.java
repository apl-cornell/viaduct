package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Boolean literal. */
public class BooleanLiteralNode extends LiteralNode<Boolean> {
  public BooleanLiteralNode(boolean value) {
    super(value);
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(bool " + this.getValue() + ")";
  }
}
