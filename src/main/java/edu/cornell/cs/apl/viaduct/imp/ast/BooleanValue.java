package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;

/** Boolean literal. */
public final class BooleanValue implements ImpValue {
  private final boolean value;

  public BooleanValue(boolean value) {
    this.value = value;
  }

  public boolean getValue() {
    return this.value;
  }

  @Override
  public ImpType getType() {
    return BooleanType.create();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof BooleanValue)) {
      return false;
    }

    final BooleanValue that = (BooleanValue) o;
    return this.value == that.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.value);
  }

  @Override
  public String toString() {
    return Boolean.toString(this.getValue());
  }
}
