package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

import java.util.Objects;

/** declare and assign temporary variables. */
public final class LetBindingNode extends StmtNode {
  private final Variable variable;
  private final ExpressionNode rhs;

  public LetBindingNode(Variable var, ExpressionNode rhs) {
    this.variable = var;
    this.rhs = rhs;
  }

  public Variable getVariable() {
    return this.variable;
  }

  public ExpressionNode getRhs() {
    return this.rhs;
  }

  public <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof LetBindingNode)) {
      return false;
    }

    LetBindingNode that = (LetBindingNode)o;
    return Objects.equals(this.variable, that.variable)
        && Objects.equals(this.rhs, that.rhs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.variable, this.rhs);
  }

  @Override
  public String toString() {
    return String.format("(let %s as %s)", this.variable, this.rhs);
  }
}
