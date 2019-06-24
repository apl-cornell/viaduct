package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.LExprVisitor;

/** Generic interface for left-expression visitors. */
public interface LExpressionNode extends ImpAstNode {
  /** convert l-expr into an expression. */
  ExpressionNode toExpression();

  <R> R accept(LExprVisitor<R> v);
}
