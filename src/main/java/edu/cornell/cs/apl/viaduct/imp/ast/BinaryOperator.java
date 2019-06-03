package edu.cornell.cs.apl.viaduct.imp.ast;

/** Represents functions that take two values and return a value. */
public abstract class BinaryOperator {
  public abstract ImpValue evaluate(ImpValue left, ImpValue right);

  @Override
  public abstract String toString();
}
