package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** A binary operator applied two expressions. */
@AutoValue
public abstract class BinaryExpressionNode extends ExpressionNode {
  public static Builder builder() {
    return new AutoValue_BinaryExpressionNode.Builder().setDefaults();
  }

  public abstract Builder toBuilder();

  public abstract ExpressionNode getLhs();

  public abstract BinaryOperator getOperator();

  public abstract ExpressionNode getRhs();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setLhs(ExpressionNode lhs);

    public abstract Builder setOperator(BinaryOperator operator);

    public abstract Builder setRhs(ExpressionNode rhs);

    public abstract BinaryExpressionNode build();
  }
}
