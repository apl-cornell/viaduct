package edu.cornell.cs.apl.viaduct;

public interface StmtNode
{
    <R> R accept(StmtVisitor<R> v);
}