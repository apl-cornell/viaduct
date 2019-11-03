package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaStatementVisitor;

@AutoValue
public abstract class MambaWhileNode implements MambaStatementNode {
  public static Builder builder() {
    return new AutoValue_MambaWhileNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract MambaExpressionNode getGuard();

  public abstract MambaBlockNode getBody();

  @Override
  public <R> R accept(MambaStatementVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setGuard(MambaExpressionNode guard);

    public abstract Builder setBody(MambaBlockNode body);

    public abstract MambaWhileNode build();
  }
}
