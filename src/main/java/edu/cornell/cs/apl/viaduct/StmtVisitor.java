package edu.cornell.cs.apl.viaduct;

public interface StmtVisitor<R>
{
    R visit(VarDeclNode varDeclNode);
    R visit(AssignNode assignNode) throws UndeclaredVariableException;
    R visit(SeqNode seqNode);
    R visit(IfNode ifNode);
}