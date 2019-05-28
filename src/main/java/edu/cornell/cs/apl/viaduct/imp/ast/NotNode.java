package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import java.util.Objects;

/** Boolean negation. */
public class NotNode extends ExpressionNode {
  private final ExpressionNode expression;

  public NotNode(ExpressionNode expression) {
    this.expression = expression;
  }

  public ExpressionNode getExpression() {
    return expression;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof NotNode) {
      NotNode otherNot = (NotNode) other;
      return otherNot.expression.equals(this.expression);

    } else {
      return false;
    }
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
