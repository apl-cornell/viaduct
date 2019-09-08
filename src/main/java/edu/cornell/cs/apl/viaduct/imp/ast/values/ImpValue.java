package edu.cornell.cs.apl.viaduct.imp.ast.values;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;

/** The result of evaluating an {@link ExpressionNode}. */
public interface ImpValue {
  ImpBaseType getType();
}
