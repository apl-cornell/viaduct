package edu.cornell.cs.apl.viaduct;

/** generic expression interface for visitors. */
public interface ExprNode extends AstNode {
  <R> R accept(ExprVisitor<R> v);
}
