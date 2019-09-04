package edu.cornell.cs.apl.viaduct.imp.ast;

/** The result of evaluating an {@link ExpressionNode}. */
public interface ImpValue {
  ImpBaseType getType();
}
