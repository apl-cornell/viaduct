package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;

@AutoValue
public abstract class HostDeclarationNode extends TopLevelDeclarationNode {
  public static Builder builder() {
    return new AutoValue_HostDeclarationNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract HostName getName();

  public abstract Label getTrust();

  @Override
  public final <R> R accept(TopLevelDeclarationVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setName(HostName name);

    public abstract Builder setTrust(Label trust);

    public abstract HostDeclarationNode build();
  }
}
