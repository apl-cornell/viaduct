package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** Assert that an expression is true. */
public final class AssertNode implements StmtNode {
  private final ExpressionNode expression;

  public AssertNode(ExpressionNode expression) {
    this.expression = Objects.requireNonNull(expression);
  }

  public ExpressionNode getExpression() {
    return this.expression;
  }

  @Override
  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof AssertNode)) {
      return false;
    }

    final AssertNode that = (AssertNode) o;
    return Objects.equals(this.expression, that.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash("ASSERT", this.expression);
  }

  @Override
  public String toString() {
    return "(assert " + this.expression.toString() + ")";
  }
}
