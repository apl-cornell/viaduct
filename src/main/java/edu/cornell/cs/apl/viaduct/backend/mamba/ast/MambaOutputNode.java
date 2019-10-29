package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaStatementVisitor;

@AutoValue
public abstract class MambaOutputNode implements MambaStatementNode {
  public static Builder builder() {
    return new AutoValue_MambaOutputNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract int getPlayer();

  public abstract MambaExpressionNode getExpression();

  @Override
  public <R> R accept(MambaStatementVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setPlayer(int player);

    public abstract Builder setExpression(MambaExpressionNode expr);

    public abstract MambaOutputNode build();
  }
}
