package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;

/** Integer literal. */
public final class IntegerValue implements ImpValue {
  private final int value;

  public IntegerValue(int value) {
    this.value = value;
  }

  public int getValue() {
    return this.value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof IntegerValue)) {
      return false;
    }

    final IntegerValue that = (IntegerValue) o;
    return this.value == that.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.value);
  }

  @Override
  public String toString() {
    return Integer.toString(this.getValue());
  }
}
