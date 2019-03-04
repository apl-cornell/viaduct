package edu.cornell.cs.apl.viaduct;

public class EqualNode implements BinaryExprNode
{
    ExprNode lhs, rhs;

    public EqualNode(ExprNode _lhs, ExprNode _rhs)
    {
        this.lhs = _lhs;
        this.rhs = _rhs;
    }

    public ExprNode getLHS()
    {
        return this.lhs;
    }

    public ExprNode getRHS()
    {
        return this.rhs;
    }

    public <R> R accept(ExprVisitor<R> v)
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        return "(== " + this.lhs.toString() + " " + this.rhs.toString() + ")";
    }
}