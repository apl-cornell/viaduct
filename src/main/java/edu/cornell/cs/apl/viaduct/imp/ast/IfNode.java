package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** If statement. */
@AutoValue
public abstract class IfNode extends StatementNode {
  /**
   * If {@code guard} evaluates to true, execute {@code thenBranch}, otherwise, execute {@code
   * elseBranch}.
   *
   * @param guard condition to check
   * @param thenBranch statement to execute if the guard is true
   * @param elseBranch statement to execute if the guard is false
   */
  public static IfNode create(
      ExpressionNode guard, StatementNode thenBranch, StatementNode elseBranch) {
    return new AutoValue_IfNode(guard, thenBranch, elseBranch);
  }

  /**
   * Like {@link #create(ExpressionNode, StatementNode, StatementNode)}, but do nothing if the guard
   * is false.
   *
   * @param guard condition to check
   * @param thenBranch statement to execute if guard is true
   */
  public static IfNode create(ExpressionNode guard, StatementNode thenBranch) {
    return create(guard, thenBranch, BlockNode.create());
  }

  public abstract ExpressionNode getGuard();

  public abstract StatementNode getThenBranch();

  public abstract StatementNode getElseBranch();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }
}
