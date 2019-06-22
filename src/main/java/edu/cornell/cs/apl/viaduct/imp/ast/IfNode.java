package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** If statement. */
public class IfNode implements StmtNode {
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
    this.guard = Objects.requireNonNull(guard);
    this.thenBranch = Objects.requireNonNull(thenBranch);
    this.elseBranch = Objects.requireNonNull(elseBranch);
  }

  /**
   * Like {@link #IfNode(ExpressionNode, StmtNode, StmtNode)}, but do nothing if the guard is false.
   *
   * @param guard condition to check
   * @param thenBranch statement to execute if guard is true
   */
  public IfNode(ExpressionNode guard, StmtNode thenBranch) {
    this(guard, thenBranch, new BlockNode());
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

  @Override
  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof IfNode)) {
      return false;
    }

    final IfNode that = (IfNode) o;
    return Objects.equals(this.guard, that.guard)
        && Objects.equals(this.thenBranch, that.thenBranch)
        && Objects.equals(this.elseBranch, that.elseBranch);
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
