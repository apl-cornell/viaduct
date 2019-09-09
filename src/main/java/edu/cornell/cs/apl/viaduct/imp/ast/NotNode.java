package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

// TODO: add unary operators (just like we did with binary operators) and refactor this class.

/** Boolean negation. */
@AutoValue
public abstract class NotNode extends ExpressionNode {
  public static Builder builder() {
    return new AutoValue_NotNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ExpressionNode getExpression();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setExpression(ExpressionNode expression);

    public abstract NotNode build();
  }
}
