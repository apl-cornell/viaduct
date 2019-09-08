package edu.cornell.cs.apl.viaduct.imp.ast.values;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.ast.types.BooleanType;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;

/** Boolean literal. */
@AutoValue
public abstract class BooleanValue implements ImpValue {
  public static BooleanValue create(boolean value) {
    return new AutoValue_BooleanValue(value);
  }

  public abstract boolean getValue();

  @Override
  public final ImpBaseType getType() {
    return BooleanType.create();
  }

  @Override
  public final String toString() {
    return Boolean.toString(this.getValue());
  }
}
