package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import java.util.Objects;

/** Boolean negation. */
public final class NotNode implements ExpressionNode {
  private final ExpressionNode expression;

  public NotNode(ExpressionNode expression) {
    this.expression = Objects.requireNonNull(expression);
  }

  public ExpressionNode getExpression() {
    return expression;
  }

  @Override
  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof NotNode)) {
      return false;
    }

    final NotNode that = (NotNode) o;
    return Objects.equals(this.expression, that.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash("!", this.expression);
  }

  @Override
  public String toString() {
    return "(! " + this.getExpression().toString() + ")";
  }
}
