package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaExpressionVisitor;

@AutoValue
public abstract class MambaNegationNode implements MambaExpressionNode {
  public static Builder builder() {
    return new AutoValue_MambaNegationNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract MambaExpressionNode getExpression();

  @Override
  public <R> R accept(MambaExpressionVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setExpression(MambaExpressionNode expr);

    public abstract MambaNegationNode build();
  }
}
