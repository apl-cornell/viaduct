package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Integer literal. */
public class IntegerLiteralNode extends LiteralNode<Integer> implements ImpValue {

  public IntegerLiteralNode(int value) {
    super(value);
  }

  @Override
  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final IntegerLiteralNode that = (IntegerLiteralNode) o;
    return this.value.equals(that.value);
  }

  @Override
  public String toString() {
    return "(int " + this.getValue() + ")";
  }
}
