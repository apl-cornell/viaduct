package edu.cornell.cs.apl.viaduct;

public class BoolLiteralNode implements ExprNode
{
    boolean val;

    public BoolLiteralNode(boolean _val)
    {
        this.val = _val;
    }

    public boolean getVal()
    {
        return this.val;
    }

    public <R> R accept(ExprVisitor<R> v)
    {
        return v.visit(this);
    }
}