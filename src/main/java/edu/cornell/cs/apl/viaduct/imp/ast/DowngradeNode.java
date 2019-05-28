package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.Objects;

/** Reduce the confidentiality and/or integrity of the result of an expression. */
public class DowngradeNode extends ExpressionNode {
  private final ExpressionNode expression;
  private final Label label;

  public DowngradeNode(ExpressionNode expression, Label label) {
    this.expression = expression;
    this.label = label;
  }

  public ExpressionNode getExpression() {
    return expression;
  }

  public Label getLabel() {
    return label;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof DowngradeNode) {
      DowngradeNode otherDowngrade = (DowngradeNode) other;
      return otherDowngrade.expression.equals(this.expression)
          && otherDowngrade.label.equals(this.label);

    } else {
      return false;
    }
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
