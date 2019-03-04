package edu.cornell.cs.apl.viaduct;

import java.util.List;

public class SeqNode implements StmtNode
{
    List<StmtNode> stmts;

    public SeqNode(List<StmtNode> _stmts)
    {
        this.stmts = _stmts;
    }

    public List<StmtNode> getStmts()
    {
        return this.stmts;
    }

    public <R> R accept(StmtVisitor<R> v)
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        String seqStr = "(";
        for (StmtNode stmt : this.stmts) 
        {
            seqStr += stmt.toString();
        }
        seqStr += ")";
        return seqStr;
    }
}