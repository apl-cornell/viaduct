package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaStatementVisitor;

@AutoValue
public abstract class MambaArrayDeclarationNode implements MambaStatementNode {
  public static Builder builder() {
    return new AutoValue_MambaArrayDeclarationNode.Builder();
  }

  public abstract MambaVariable getVariable();

  public abstract MambaExpressionNode getLength();

  public abstract MambaSecurityType getRegisterType();

  public abstract Builder toBuilder();

  @Override
  public <R> R accept(MambaStatementVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setVariable(MambaVariable var);

    public abstract Builder setLength(MambaExpressionNode expr);

    public abstract Builder setRegisterType(MambaSecurityType type);

    public abstract MambaArrayDeclarationNode build();
  }
}
