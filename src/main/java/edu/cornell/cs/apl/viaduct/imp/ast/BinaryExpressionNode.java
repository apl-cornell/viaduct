package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** A binary operator applied two expressions. */
@AutoValue
public abstract class BinaryExpressionNode extends ExpressionNode {
  /** Create a binary expression given the operator and its two arguments. */
  public static BinaryExpressionNode create(
      ExpressionNode lhs, BinaryOperator operator, ExpressionNode rhs) {
    return new AutoValue_BinaryExpressionNode(lhs, operator, rhs);
  }

  public abstract ExpressionNode getLhs();

  public abstract BinaryOperator getOperator();

  public abstract ExpressionNode getRhs();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }
}
