package edu.cornell.cs.apl.viaduct;

/** variable reference. */
public class VarLookupNode implements ExprNode {
  Variable var;

  public VarLookupNode(Variable var) {
    this.var = var;
  }

  public Variable getVar() {
    return this.var;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(var " + var.toString() + ")";
  }
}
