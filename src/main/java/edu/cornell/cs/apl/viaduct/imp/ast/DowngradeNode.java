package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;

/** Reduce the confidentiality and/or integrity of the result of an expression. */
@AutoValue
public abstract class DowngradeNode extends ExpressionNode {
  public static DowngradeNode create(ExpressionNode expression, Label label) {
    return new AutoValue_DowngradeNode(expression, label);
  }

  public abstract ExpressionNode getExpression();

  public abstract Label getLabel();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }
}
