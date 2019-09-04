package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;

/** Type of binary operators. */
@AutoValue
public abstract class BinaryOperatorType {
  public static BinaryOperatorType create(
      ImpBaseType returnType, ImpBaseType lhsType, ImpBaseType rhsType) {
    return new AutoValue_BinaryOperatorType(returnType, lhsType, rhsType);
  }

  public abstract ImpBaseType getReturnType();

  public abstract ImpBaseType getLhsType();

  public abstract ImpBaseType getRhsType();
}
