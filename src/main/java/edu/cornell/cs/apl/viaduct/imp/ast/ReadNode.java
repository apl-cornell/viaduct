package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Read the value pointed to by a reference. */
@AutoValue
public abstract class ReadNode extends ExpressionNode {
  public static ReadNode create(ReferenceNode ref) {
    return builder().setReference(ref).build();
  }

  public static Builder builder() {
    return new AutoValue_ReadNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ReferenceNode getReference();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setReference(ReferenceNode reference);

    public abstract ReadNode build();
  }
}
