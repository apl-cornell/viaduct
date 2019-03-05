package edu.cornell.cs.apl.viaduct.surface;

import edu.cornell.cs.apl.viaduct.ExprVisitor;

/** Generic interface for expression visitors. */
public interface ExpressionNode extends AstNode {
  <R> R accept(ExprVisitor<R> v);
}
