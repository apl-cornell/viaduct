package edu.cornell.cs.apl.viaduct;

/** variable assignment statement. */
public class AssignNode implements StmtNode {
  Variable var;
  ExprNode rhs;

  public AssignNode(Variable var, ExprNode rhs) {
    this.var = var;
    this.rhs = rhs;
  }

  public Variable getVar() {
    return this.var;
  }

  public ExprNode getRhs() {
    return this.rhs;
  }

  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(assign " + this.var.toString() + " to " + this.rhs.toString() + ")";
  }
}
