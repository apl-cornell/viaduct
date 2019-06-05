package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import java.util.Objects;

/** Read the value of a variable. */
public final class ReadNode extends ExpressionNode {
  private final Variable variable;

  public ReadNode(Variable variable) {
    this.variable = Objects.requireNonNull(variable);
  }

  public ReadNode(Binding<ImpAstNode> binding) {
    this.variable = new Variable(Objects.requireNonNull(binding));
  }

  public Variable getVariable() {
    return variable;
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

    if (!(o instanceof ReadNode)) {
      return false;
    }

    final ReadNode that = (ReadNode) o;
    return Objects.equals(this.variable, that.variable);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(variable);
  }

  @Override
  public String toString() {
    return "(var " + this.getVariable().toString() + ")";
  }
}
