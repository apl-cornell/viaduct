package edu.cornell.cs.apl.viaduct.imp.ast.types;

import com.google.auto.value.AutoValue;

/** Type of arrays containing elements of a single type. */
@AutoValue
public abstract class ArrayType implements ImpType {
  /** Represents the type {@code elementType[]}. */
  public static ArrayType create(ImpBaseType elementType) {
    return new AutoValue_ArrayType(elementType);
  }

  public abstract ImpBaseType getElementType();

  @Override
  public String toString() {
    return getElementType().toString() + "[]";
  }
}
