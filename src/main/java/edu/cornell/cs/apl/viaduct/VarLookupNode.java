package edu.cornell.cs.apl.viaduct;

public class VarLookupNode implements ExprNode
{
    Variable var;

    public VarLookupNode(Variable _var)
    {
        this.var = _var;
    }

    public Variable getVar()
    {
        return this.var;
    }

    public <R> R accept(ExprVisitor<R> v)
    {
        // TODO: ew fix this
        try {
            return v.visit(this);
        } catch (UndeclaredVariableException undeclVarExn) {}

        return null;
    }
}