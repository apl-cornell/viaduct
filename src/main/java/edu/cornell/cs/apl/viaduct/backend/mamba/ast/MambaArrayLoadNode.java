package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaExpressionVisitor;

@AutoValue
public abstract class MambaArrayLoadNode implements MambaExpressionNode {
  public static Builder builder() {
    return new AutoValue_MambaArrayLoadNode.Builder();
  }

  public abstract MambaVariable getArray();

  public abstract MambaExpressionNode getIndex();

  public abstract Builder toBuilder();

  @Override
  public <R> R accept(MambaExpressionVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setArray(MambaVariable var);

    public abstract Builder setIndex(MambaExpressionNode expr);

    public abstract MambaArrayLoadNode build();
  }
}
