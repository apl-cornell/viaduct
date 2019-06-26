package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import java.util.Objects;

/** Reference to an index in an array. */
public final class ArrayIndex implements Reference {
  private final Variable array;
  private final ExpressionNode index;

  public ArrayIndex(Variable array, ExpressionNode index) {
    this.array = Objects.requireNonNull(array);
    this.index = Objects.requireNonNull(index);
  }

  public Variable getArray() {
    return this.array;
  }

  public ExpressionNode getIndex() {
    return this.index;
  }

  @Override
  public <R> R accept(ReferenceVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ArrayIndex)) {
      return false;
    }

    final ArrayIndex that = (ArrayIndex) o;
    return Objects.equals(this.array, that.array) && Objects.equals(this.index, that.index);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.array, this.index);
  }

  @Override
  public String toString() {
    return String.format("%s[%s]", this.array, this.index);
  }
}
