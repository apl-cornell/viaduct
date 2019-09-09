package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;

@AutoValue
public abstract class ProcessDeclarationNode extends TopLevelDeclarationNode {
  public static Builder builder() {
    return new AutoValue_ProcessDeclarationNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ProcessName getName();

  public abstract BlockNode getBody();

  @Override
  public final <R> R accept(TopLevelDeclarationVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setName(ProcessName name);

    public abstract Builder setBody(BlockNode body);

    public abstract BlockNode.Builder bodyBuilder();

    public abstract ProcessDeclarationNode build();
  }
}
