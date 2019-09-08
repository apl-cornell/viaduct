package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAstVisitor;

/** An expression node that supports visitors. */
public abstract class ExpressionNode extends ImpAstNode {
  public abstract <R> R accept(ExprVisitor<R> v);

  @Override
  public final <R> R accept(ImpAstVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
