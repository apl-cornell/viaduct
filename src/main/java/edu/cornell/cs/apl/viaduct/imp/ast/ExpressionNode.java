package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Generic interface for expression visitors. */
public abstract class ExpressionNode extends ImpAstNode {
  public abstract <R> R accept(ExprVisitor<R> v);
}
