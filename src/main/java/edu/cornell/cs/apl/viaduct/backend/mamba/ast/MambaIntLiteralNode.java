package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaExpressionVisitor;

@AutoValue
public abstract class MambaIntLiteralNode implements MambaExpressionNode {
  public static Builder builder() {
    return new AutoValue_MambaIntLiteralNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract int getValue();

  public abstract MambaSecurityType getSecurityType();

  @Override
  public final <R> R accept(MambaExpressionVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setValue(int value);

    public abstract Builder setSecurityType(MambaSecurityType type);

    public abstract MambaIntLiteralNode build();
  }
}
