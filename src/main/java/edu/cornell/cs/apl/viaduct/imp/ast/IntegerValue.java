package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;

/** Integer literal. */
@AutoValue
public abstract class IntegerValue implements ImpValue {
  public static IntegerValue create(int value) {
    return new AutoValue_IntegerValue(value);
  }

  public abstract int getValue();

  @Override
  public final ImpType getType() {
    return IntegerType.create();
  }

  @Override
  public final String toString() {
    return Integer.toString(this.getValue());
  }
}
