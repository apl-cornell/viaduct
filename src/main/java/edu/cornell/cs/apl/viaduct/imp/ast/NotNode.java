package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

// TODO: add unary operators (just like we did with binary operators) and refactor this class.

/** Boolean negation. */
@AutoValue
public abstract class NotNode extends ExpressionNode {
  public static NotNode create(ExpressionNode expression) {
    return new AutoValue_NotNode(expression);
  }

  public abstract ExpressionNode getExpression();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }
}
