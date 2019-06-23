package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.Binding;
import java.util.Objects;

/** Read the value of a variable. */
public abstract class AbstractReadNode {
  protected final Variable variable;

  public AbstractReadNode(Variable variable) {
    this.variable = Objects.requireNonNull(variable);
  }

  public AbstractReadNode(Binding<ImpAstNode> binding) {
    this.variable = new Variable(Objects.requireNonNull(binding));
  }

  public AbstractReadNode(String name) {
    this.variable = new Variable(name);
  }

  public Variable getVariable() {
    return variable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof AbstractReadNode)) {
      return false;
    }

    final AbstractReadNode that = (AbstractReadNode) o;
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
