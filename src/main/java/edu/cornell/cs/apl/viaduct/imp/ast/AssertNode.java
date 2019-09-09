package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Assert that an expression is true. */
@AutoValue
public abstract class AssertNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_AssertNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ExpressionNode getExpression();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setExpression(ExpressionNode expression);

    public abstract AssertNode build();
  }
}
