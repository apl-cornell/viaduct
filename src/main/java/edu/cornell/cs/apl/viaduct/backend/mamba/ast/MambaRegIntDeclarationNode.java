package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaStatementVisitor;

@AutoValue
public abstract class MambaRegIntDeclarationNode implements MambaStatementNode {
  public static Builder builder() {
    return new AutoValue_MambaRegIntDeclarationNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract MambaVariable getVariable();

  public abstract MambaSecurityType getRegisterType();

  @Override
  public <R> R accept(MambaStatementVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setVariable(MambaVariable variable);

    public abstract Builder setRegisterType(MambaSecurityType regType);

    public abstract MambaRegIntDeclarationNode build();
  }
}
