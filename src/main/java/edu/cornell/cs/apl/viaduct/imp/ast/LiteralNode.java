package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import java.util.Objects;

/** A literal constant. */
public final class LiteralNode implements ExpressionNode {
  private final ImpValue value;

  public LiteralNode(ImpValue value) {
    this.value = Objects.requireNonNull(value);
  }

  public ImpValue getValue() {
    return value;
  }

  @Override
  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return this.value.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof LiteralNode)) {
      return false;
    }

    final LiteralNode that = (LiteralNode) o;
    return Objects.equals(this.value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.value);
  }
}
