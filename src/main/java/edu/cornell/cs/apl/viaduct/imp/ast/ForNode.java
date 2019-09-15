package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import javax.annotation.Nullable;

/** For loop. */
@AutoValue
public abstract class ForNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_ForNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract @Nullable JumpLabel getJumpLabel();

  /** Initializer for loop variables. */
  public abstract StatementNode getInitialize();

  /** Loop until this becomes false. */
  public abstract ExpressionNode getGuard();

  /** Statement that updates loop variables. */
  public abstract StatementNode getUpdate();

  /** Code to execute each time we go around the loop. */
  public abstract BlockNode getBody();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setInitialize(StatementNode initialize);

    public abstract Builder setJumpLabel(JumpLabel jumpLabel);

    public abstract Builder setGuard(ExpressionNode guard);

    public abstract Builder setUpdate(StatementNode update);

    public abstract Builder setBody(BlockNode body);

    public abstract BlockNode.Builder bodyBuilder();

    public abstract ForNode build();
  }
}
