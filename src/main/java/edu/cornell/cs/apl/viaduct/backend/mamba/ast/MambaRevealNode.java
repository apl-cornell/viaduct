package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaExpressionVisitor;

@AutoValue
public abstract class MambaRevealNode implements MambaExpressionNode {
  public static Builder builder() {
    return new AutoValue_MambaRevealNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract MambaExpressionNode getRevealedExpr();

  @Override
  public final <R> R accept(MambaExpressionVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setRevealedExpr(MambaExpressionNode expr);

    public abstract MambaRevealNode build();
  }
}
