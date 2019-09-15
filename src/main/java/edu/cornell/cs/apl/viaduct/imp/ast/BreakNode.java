package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import javax.annotation.Nullable;

@AutoValue
public abstract class BreakNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_BreakNode.Builder();
  }

  public abstract Builder toBuilder();

  /** Label of the loop to break out of. {@code null} means break out of the innermost loop. */
  public abstract @Nullable JumpLabel getJumpLabel();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    /** Set the label of the loop to break out of. Defaults to the closest loop. */
    public abstract Builder setJumpLabel(JumpLabel jumpLabel);

    public abstract BreakNode build();
  }
}
