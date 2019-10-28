package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;

import javax.annotation.Nullable;

@AutoValue
public abstract class ProcessDeclarationNode extends TopLevelDeclarationNode {
  public static Builder builder() {
    return new AutoValue_ProcessDeclarationNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ProcessName getName();

  public abstract BlockNode getBody();

  @Nullable
  public abstract Protocol<? extends AstNode> getProtocol();

  @Override
  public final <R> R accept(TopLevelDeclarationVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setName(ProcessName name);

    public abstract Builder setBody(BlockNode body);

    public abstract Builder setProtocol(Protocol<? extends AstNode> protocol);

    public abstract BlockNode.Builder bodyBuilder();

    public abstract ProcessDeclarationNode build();
  }
}
