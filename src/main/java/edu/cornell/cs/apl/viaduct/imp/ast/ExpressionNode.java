package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.parser.SourceRange;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** An expression node that supports visitors. */
public abstract class ExpressionNode extends ImpAstNode {
  public abstract <R> R accept(ExprVisitor<R> v);

  @Override
  public ExpressionNode setSourceLocation(SourceRange sourceLocation) {
    super.setSourceLocation(sourceLocation);
    return this;
  }
}
