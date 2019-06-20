package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

import java.util.Objects;

public final class ArrayAccessNode extends ExpressionNode {
  private final Variable variable;
  private final ExpressionNode index;

  /**
   * access array at a particular index.
   *
   * @param v name of the array
   * @param ind array index to access
   */
  public ArrayAccessNode(Variable v, ExpressionNode ind) {
    this.variable = v;
    this.index = ind;
  }

  public Variable getVariable() {
    return this.variable;
  }

  public ExpressionNode getIndex() {
    return this.index;
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ArrayAccessNode)) {
      return false;
    }

    final ArrayAccessNode that = (ArrayAccessNode) o;
    return Objects.equals(this.variable, that.variable)
        && Objects.equals(this.index, that.index);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.variable, this.index);
  }

  @Override
  public String toString() {
    return String.format("(arrayAccess %s at %s)", this.variable, this.index);
  }
}
