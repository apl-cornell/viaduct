package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

import java.util.Objects;

/** If statement. */
public class IfNode extends StmtNode {
  private final ExpressionNode guard;
  private final StmtNode thenBranch;
  private final StmtNode elseBranch;

  /**
   * If {@code guard} evaluates to true, execute {@code thenBranch}, otherwise, execute {@code
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
  public boolean equals(Object other) {
    if (other == null) { return false; }

    if (other instanceof IfNode) {
      IfNode otherIf = (IfNode) other;
      return otherIf.guard.equals(this.guard)
          && otherIf.thenBranch.equals(this.thenBranch)
          && otherIf.elseBranch.equals(this.elseBranch);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.guard, this.thenBranch, this.elseBranch);
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
