package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Unguarded loop. */
@AutoValue
public abstract class LoopNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_LoopNode.Builder().setDefaults();
  }

  public abstract Builder toBuilder();

  public abstract BlockNode getBody();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setBody(BlockNode body);

    public abstract BlockNode.Builder bodyBuilder();

    public abstract LoopNode build();
  }
}
