package edu.cornell.cs.apl.viaduct;

public interface StmtVisitor<R>
{
    R visit(SkipNode skipNode);
    R visit(VarDeclNode varDeclNode);
    R visit(AssignNode assignNode);
    R visit(SeqNode seqNode);
    R visit(IfNode ifNode);
}