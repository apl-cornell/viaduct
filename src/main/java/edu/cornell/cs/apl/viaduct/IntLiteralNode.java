package edu.cornell.cs.apl.viaduct;

public class IntLiteralNode implements ExprNode
{
    int val;

    public IntLiteralNode(int _val)
    {
        this.val = _val;
    }

    public int getVal()
    {
        return this.val;
    }

    public <R> R accept(ExprVisitor<R> v)
    {
        return v.visit(this);
    }
}