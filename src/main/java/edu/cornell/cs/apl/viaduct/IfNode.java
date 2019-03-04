package edu.cornell.cs.apl.viaduct;

/** conditional statements. */
public class IfNode implements StmtNode {
  ExprNode guard;
  StmtNode thenBranch;
  StmtNode elseBranch;

  /** default constructor. */
  public IfNode(ExprNode guard, StmtNode thenBranch, StmtNode elseBranch) {
    this.guard = guard;
    this.thenBranch = thenBranch;
    this.elseBranch = elseBranch;
  }

  public ExprNode getGuard() {
    return this.guard;
  }

  public StmtNode getThenBranch() {
    return this.thenBranch;
  }

  public StmtNode getElseBranch() {
    return this.elseBranch;
  }

  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(if "
        + this.guard.toString()
        + " then "
        + this.thenBranch
        + " else "
        + this.elseBranch
        + ")";
  }
}
