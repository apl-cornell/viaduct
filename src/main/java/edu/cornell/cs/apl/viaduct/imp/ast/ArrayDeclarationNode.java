package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Objects;

public final class ArrayDeclarationNode implements StmtNode {
  private final Variable variable;
  private final ExpressionNode length;
  private final ImpType type;
  private final Label label;

  /**
   * Declare a statically allocated array with the given length.
   *
   * @param variable name of the array
   * @param length number of elements in the array
   * @param type type of the elements in the array
   * @param label security label of the array and all its elements
   */
  public ArrayDeclarationNode(Variable variable, ExpressionNode length, ImpType type, Label label) {
    this.variable = Objects.requireNonNull(variable);
    this.length = Objects.requireNonNull(length);
    this.type = Objects.requireNonNull(type);
    this.label = Objects.requireNonNull(label);
  }

  public Variable getVariable() {
    return variable;
  }

  public ImpType getType() {
    return type;
  }

  public Label getLabel() {
    return label;
  }

  public ExpressionNode getLength() {
    return length;
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

    if (!(o instanceof ArrayDeclarationNode)) {
      return false;
    }

    final ArrayDeclarationNode that = (ArrayDeclarationNode) o;
    return Objects.equals(this.variable, that.variable)
        && Objects.equals(this.length, that.length)
        && Objects.equals(this.type, that.type)
        && Objects.equals(this.label, that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.variable, this.length, this.type, this.label);
  }

  @Override
  public String toString() {
    return String.format(
        "(arrayDeclaration %s[%s] as %s %s)", this.variable, this.length, this.type, this.label);
  }
}
