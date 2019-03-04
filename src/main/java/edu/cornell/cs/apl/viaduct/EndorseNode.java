package edu.cornell.cs.apl.viaduct;

public class EndorseNode implements ExprNode
{
    ExprNode endorsedExpr;
    Label downgradeLabel;

    public EndorseNode(ExprNode endoExpr, Label label)
    {
        this.endorsedExpr = endoExpr;
        this.downgradeLabel = label;
    }

    public ExprNode getEndorsedExpr()
    {
        return this.endorsedExpr;
    }

    public Label getDowngradeLabel()
    {
        return this.downgradeLabel;
    }

    public <R> R accept(ExprVisitor<R> v)
    {
        return v.visit(this);
    }

    @Override
    public String toString()
    {
        return "(endorse " + this.endorsedExpr.toString() + " to " + this.downgradeLabel.toString() + ")";
    }
}