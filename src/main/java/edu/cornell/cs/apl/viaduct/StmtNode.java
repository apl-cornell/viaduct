package edu.cornell.cs.apl.viaduct;

public interface StmtNode extends ASTNode
{
    <R> R accept(StmtVisitor<R> v);
}