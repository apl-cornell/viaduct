package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaExpressionVisitor;

@AutoValue
public abstract class MambaBinaryExpressionNode implements MambaExpressionNode {
  public static Builder builder() {
    return new AutoValue_MambaBinaryExpressionNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract MambaExpressionNode getLhs();

  public abstract MambaExpressionNode getRhs();

  public abstract MambaBinaryOperator getOperator();

  @Override
  public final <R> R accept(MambaExpressionVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setLhs(MambaExpressionNode lhs);

    public abstract Builder setRhs(MambaExpressionNode rhs);

    public abstract Builder setOperator(MambaBinaryOperator operator);

    public abstract MambaBinaryExpressionNode build();
  }
}
