package edu.cornell.cs.apl.viaduct.imp.typing;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpType;

/**
 * Type of arrays where element type is not specified.
 *
 * <p>Used by the type checker for error reporting when a reference is expected to resolve to an
 * array with <em>some</em> element type.
 */
final class RawArrayType implements ImpType {
  private static final RawArrayType INSTANCE = new RawArrayType();

  private RawArrayType() {}

  public static RawArrayType create() {
    return INSTANCE;
  }

  @Override
  public boolean equals(Object other) {
    return this == other;
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "Array";
  }
}
