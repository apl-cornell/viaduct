package edu.cornell.cs.apl.viaduct.imp.ast;

/**
 * Explicitly encodes a value that is never provided (as opposed to a value implicitly never being
 * provided). A program that tries to reduce an unavailable value gets stuck.
 */
public final class UnavailableValue implements ImpValue {
  private static final UnavailableValue INSTANCE = new UnavailableValue();

  private UnavailableValue() {}

  public static UnavailableValue create() {
    return INSTANCE;
  }

  @Override
  public ImpType getType() {
    throw new RuntimeException(this.toString() + " values do not have a specific type.");
  }

  @Override
  public String toString() {
    return "unavailable";
  }
}
