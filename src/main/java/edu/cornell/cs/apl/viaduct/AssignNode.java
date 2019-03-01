package edu.cornell.cs.apl.viaduct;

public class AssignNode implements StmtNode
{
    Variable var;
    ExprNode rhs;

    public AssignNode(Variable _var, ExprNode _rhs)
    {
        this.var = _var;
        this.rhs = _rhs;
    }

    public Variable getVar()
    {
        return this.var;
    }

    public ExprNode getRHS()
    {
        return this.rhs;
    }

    public <R> R accept(StmtVisitor<R> v)
    {
        return v.visit(this);
    }
}