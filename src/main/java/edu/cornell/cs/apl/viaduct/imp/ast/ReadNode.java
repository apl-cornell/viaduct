package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import java.util.Objects;

/** Read the value pointed to by a reference. */
public final class ReadNode implements ExpressionNode {
  private final Reference reference;

  public ReadNode(Reference reference) {
    this.reference = Objects.requireNonNull(reference);
  }

  public Reference getReference() {
    return reference;
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

    if (!(o instanceof ReadNode)) {
      return false;
    }

    final ReadNode that = (ReadNode) o;
    return Objects.equals(this.reference, that.reference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.reference);
  }

  @Override
  public String toString() {
    return String.format("(read %s)", this.reference);
  }
}
