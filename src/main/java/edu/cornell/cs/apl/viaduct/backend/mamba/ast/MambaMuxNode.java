package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaExpressionVisitor;

@AutoValue
public abstract class MambaMuxNode implements MambaExpressionNode {
  public static Builder builder() {
    return new AutoValue_MambaMuxNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract MambaExpressionNode getGuard();

  public abstract MambaExpressionNode getThenValue();

  public abstract MambaExpressionNode getElseValue();

  @Override
  public <R> R accept(MambaExpressionVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setGuard(MambaExpressionNode expr);

    public abstract Builder setThenValue(MambaExpressionNode expr);

    public abstract Builder setElseValue(MambaExpressionNode expr);

    public abstract MambaMuxNode build();
  }
}
