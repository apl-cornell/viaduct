package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** If statement. */
public class IfNode implements StmtNode {
  private final ExpressionNode guard;
  private final StmtNode thenBranch;
  private final StmtNode elseBranch;

  /**
   * If {@param guard} evaluates to true, execute {@param thenBranch}, otherwise, execute {@param
   * elseBranch}.
   *
   * @param guard condition to check
   * @param thenBranch statement to execute if the guard is true
   * @param elseBranch statement to execute if the guard is false
   */
  public IfNode(ExpressionNode guard, StmtNode thenBranch, StmtNode elseBranch) {
    this.guard = guard;
    this.thenBranch = thenBranch;
    this.elseBranch = elseBranch;
  }

  public ExpressionNode getGuard() {
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
        + this.getGuard().toString()
        + " then "
        + this.getThenBranch()
        + " else "
        + this.getElseBranch()
        + ")";
  }
}
