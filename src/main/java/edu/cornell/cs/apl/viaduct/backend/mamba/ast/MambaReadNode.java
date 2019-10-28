package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaExpressionVisitor;

@AutoValue
public abstract class MambaReadNode implements MambaExpressionNode {
  public static MambaReadNode create(MambaVariable var) {
    return builder().setVariable(var).build();
  }

  public static Builder builder() {
    return new AutoValue_MambaReadNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract MambaVariable getVariable();

  @Override
  public final <R> R accept(MambaExpressionVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setVariable(MambaVariable var);

    public abstract MambaReadNode build();
  }
}
