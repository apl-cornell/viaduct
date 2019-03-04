package edu.cornell.cs.apl.viaduct;

public class IfNode implements StmtNode
{
    ExprNode guard;
    StmtNode thenBranch;
    StmtNode elseBranch;

    public IfNode(ExprNode _guard, StmtNode _then, StmtNode _else)
    {
        this.guard = _guard;
        this.thenBranch = _then;
        this.elseBranch = _else;
    }

    public ExprNode getGuard()
    {
        return this.guard;
    }

    public StmtNode getThenBranch()
    {
        return this.thenBranch;
    }

    public StmtNode getElseBranch()
    {
        return this.elseBranch;
    }

    public <R> R accept(StmtVisitor<R> v)
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        return "(if " + this.guard.toString()
                + " then " + this.thenBranch
                + " else " + this.elseBranch
                + ")";
    }
}