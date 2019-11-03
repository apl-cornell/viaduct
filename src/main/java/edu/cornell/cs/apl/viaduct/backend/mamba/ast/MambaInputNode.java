package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaStatementVisitor;

@AutoValue
public abstract class MambaInputNode implements MambaStatementNode {
  public static Builder builder() {
    return new AutoValue_MambaInputNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract MambaVariable getVariable();

  public abstract int getPlayer();

  public abstract MambaSecurityType getSecurityContext();

  @Override
  public <R> R accept(MambaStatementVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setVariable(MambaVariable var);

    public abstract Builder setPlayer(int player);

    public abstract Builder setSecurityContext(MambaSecurityType type);

    public abstract MambaInputNode build();
  }
}
