package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Objects;

/** Reduce the confidentiality and/or integrity of the result of an expression. */
public final class DowngradeNode implements ExpressionNode {
  private final ExpressionNode expression;
  private final Label label;

  public DowngradeNode(ExpressionNode expression, Label label) {
    this.expression = Objects.requireNonNull(expression);
    this.label = Objects.requireNonNull(label);
  }

  public ExpressionNode getExpression() {
    return expression;
  }

  public Label getLabel() {
    return label;
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

    if (!(o instanceof DowngradeNode)) {
      return false;
    }

    final DowngradeNode that = (DowngradeNode) o;
    return Objects.equals(this.expression, that.expression)
        && Objects.equals(this.label, that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expression, this.label);
  }

  @Override
  public String toString() {
    return "(downgrade "
        + this.getExpression().toString()
        + " to "
        + this.getLabel().toString()
        + ")";
  }
}
