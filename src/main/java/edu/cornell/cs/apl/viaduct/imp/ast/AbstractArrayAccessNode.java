package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;

public abstract class AbstractArrayAccessNode {
  protected final Variable variable;
  protected final ExpressionNode index;

  /**
   * access array at a particular index.
   *
   * @param v name of the array
   * @param ind array index to access
   */
  public AbstractArrayAccessNode(Variable v, ExpressionNode ind) {
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof AbstractArrayAccessNode)) {
      return false;
    }

    final AbstractArrayAccessNode that = (AbstractArrayAccessNode) o;
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
