package edu.cornell.cs.apl.viaduct;

public class LessThanNode implements ExprNode
{
    ExprNode lhs, rhs;

    public LessThanNode(ExprNode _lhs, ExprNode _rhs) 
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
}