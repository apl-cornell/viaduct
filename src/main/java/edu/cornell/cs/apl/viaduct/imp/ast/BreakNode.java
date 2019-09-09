package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

@AutoValue
public abstract class BreakNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_BreakNode.Builder().setLevel(1);
  }

  public abstract Builder toBuilder();

  /** Number of loops to break out of. */
  // TODO: this should REALLY be a label.
  public abstract int getLevel();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    /** Set the number of levels to break out of. Defaults to 1. */
    public abstract Builder setLevel(int level);

    abstract BreakNode autoBuild();

    /**
     * Construct a break node.
     *
     * @throws IllegalArgumentException if {@code level} is set to a value less than 1
     */
    public final BreakNode build() {
      final BreakNode node = this.autoBuild();
      Preconditions.checkArgument(node.getLevel() >= 1, "Break level must be at least 1.");
      return node;
    }
  }
}
