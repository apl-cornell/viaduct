package edu.cornell.cs.apl.viaduct;

public interface ExprVisitor<R>
{
    R visit(VarLookupNode var) throws UndeclaredVariableException;
    R visit(IntLiteralNode intLit);
    R visit(PlusNode plusNode);
    R visit(BoolLiteralNode boolLit);
    R visit(OrNode orNode);
    R visit(AndNode andNode);
    R visit(LessThanNode ltNode);
    R visit(EqualNode eqNode);
    R visit(LeqNode leqNode);
    R visit(NotNode notNode);
    R visit(DeclassifyNode declNode);
    R visit(EndorseNode endoNode);
}
