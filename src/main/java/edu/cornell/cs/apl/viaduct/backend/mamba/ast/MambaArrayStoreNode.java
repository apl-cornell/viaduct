package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaStatementVisitor;

@AutoValue
public abstract class MambaArrayStoreNode implements MambaStatementNode {
  public static Builder builder() {
    return new AutoValue_MambaArrayStoreNode.Builder();
  }

  public abstract MambaVariable getArray();

  public abstract MambaExpressionNode getIndex();

  public abstract MambaExpressionNode getValue();

  public abstract Builder toBuilder();

  @Override
  public <R> R accept(MambaStatementVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setArray(MambaVariable var);

    public abstract Builder setIndex(MambaExpressionNode expr);

    public abstract Builder setValue(MambaExpressionNode expr);

    public abstract MambaArrayStoreNode build();
  }
}
