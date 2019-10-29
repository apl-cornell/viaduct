package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaStatementVisitor;

@AutoValue
public abstract class MambaAssignNode implements MambaStatementNode {
  public static Builder builder() {
    return new AutoValue_MambaAssignNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract MambaVariable getVariable();

  public abstract MambaExpressionNode getRhs();

  @Override
  public final <R> R accept(MambaStatementVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setVariable(MambaVariable var);

    public abstract Builder setRhs(MambaExpressionNode rhs);

    public abstract MambaAssignNode build();
  }
}
