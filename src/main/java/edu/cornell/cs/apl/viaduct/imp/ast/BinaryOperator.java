package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.ast.types.BinaryOperatorType;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;

/** Represents functions that take two values and return a value. */
public abstract class BinaryOperator {
  public abstract ImpValue evaluate(ImpValue lhs, ImpValue rhs);

  public abstract BinaryOperatorType getType();

  // Force override in subclasses.
  @Override
  public abstract String toString();
}
