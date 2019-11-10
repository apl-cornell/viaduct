package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** If statement. */
@AutoValue
public abstract class IfNode extends StatementNode implements ControlNode {
  public static Builder builder() {
    return new AutoValue_IfNode.Builder().setLoopGuard(false);
  }

  public abstract Builder toBuilder();

  public abstract ExpressionNode getGuard();

  /** Statement to execute if the guard is true. */
  public abstract BlockNode getThenBranch();

  /** Statement to execute if the guard is false. */
  public abstract BlockNode getElseBranch();

  /** true when the conditional is an elaborated loop guard. */
  public abstract boolean isLoopGuard();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setGuard(ExpressionNode guard);

    public abstract Builder setThenBranch(BlockNode body);

    public abstract Builder setElseBranch(BlockNode body);

    public abstract Builder setLoopGuard(boolean loopGuard);

    public abstract BlockNode.Builder thenBranchBuilder();

    public abstract BlockNode.Builder elseBranchBuilder();

    public abstract IfNode build();
  }
}
