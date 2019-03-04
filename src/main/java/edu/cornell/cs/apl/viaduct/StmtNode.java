package edu.cornell.cs.apl.viaduct;

/** generic statement interface for visitors. */
public interface StmtNode extends AstNode {
  <R> R accept(StmtVisitor<R> v);
}
