package edu.cornell.cs.apl.viaduct;

public interface ExprNode extends ASTNode
{
  <R> R accept(ExprVisitor<R> v);
}
