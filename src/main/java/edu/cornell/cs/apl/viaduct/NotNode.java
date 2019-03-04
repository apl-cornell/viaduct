package edu.cornell.cs.apl.viaduct;

public class NotNode implements ExprNode
{
    ExprNode negatedExpr;

    public NotNode(ExprNode nexpr)
    {
        this.negatedExpr = nexpr;
    }

    public ExprNode getNegatedExpr()
    {
        return this.negatedExpr;
    }

    public <R> R accept(ExprVisitor<R> v)
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        return "(! " + this.negatedExpr.toString() + ")";
    }
}