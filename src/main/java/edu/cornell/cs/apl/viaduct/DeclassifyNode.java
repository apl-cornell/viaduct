package edu.cornell.cs.apl.viaduct;

/** declassify expression to a downgraded label. */
public class DeclassifyNode implements ExprNode {
  ExprNode declassifiedExpr;
  Label downgradeLabel;

  public DeclassifyNode(ExprNode declExpr, Label label) {
    this.declassifiedExpr = declExpr;
    this.downgradeLabel = label;
  }

  public ExprNode getDeclassifiedExpr() {
    return this.declassifiedExpr;
  }

  public Label getDowngradeLabel() {
    return this.downgradeLabel;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(declassify "
        + this.declassifiedExpr.toString()
        + " to "
        + this.downgradeLabel.toString()
        + ")";
  }
}
