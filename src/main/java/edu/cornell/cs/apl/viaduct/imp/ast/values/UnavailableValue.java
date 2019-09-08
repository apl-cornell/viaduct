package edu.cornell.cs.apl.viaduct.imp.ast.values;

import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;

/**
 * Explicitly encodes a value that is never provided (as opposed to a value implicitly never being
 * provided). A program that tries to reduce an unavailable value gets stuck.
 */
// TODO: replace with default values
public final class UnavailableValue implements ImpValue {
  private static final UnavailableValue INSTANCE = new UnavailableValue();

  private UnavailableValue() {}

  public static UnavailableValue create() {
    return INSTANCE;
  }

  @Override
  public ImpBaseType getType() {
    throw new RuntimeException(this.toString() + " values do not have a specific type.");
  }

  @Override
  public String toString() {
    return "unavailable";
  }
}
