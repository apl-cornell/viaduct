package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.TypeCheckException;

/** Represents functions that take two values and return a value. */
public abstract class BinaryOperator {
  public abstract ImpValue evaluate(ImpValue left, ImpValue right);

  public abstract ImpType typeCheck(ImpType lhs, ImpType rhs) throws TypeCheckException;

  @Override
  public abstract String toString();
}
