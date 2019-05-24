package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Boolean literal. */
public class BooleanLiteralNode extends LiteralNode<Boolean> implements ImpValue {

  public BooleanLiteralNode(boolean value) {
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

    final BooleanLiteralNode that = (BooleanLiteralNode) o;
    return this.value.equals(that.value);
  }

  @Override
  public String toString() {
    return "(bool " + this.getValue() + ")";
  }
}
