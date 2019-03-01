package edu.cornell.cs.apl.viaduct;

public interface ExprNode
{
  <R> R accept(ExprVisitor<R> v);
}
