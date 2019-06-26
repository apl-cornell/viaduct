package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Objects;

/** Variable declaration. */
public final class VariableDeclarationNode implements StmtNode {
  private final Variable variable;
  private final ImpType type;
  private final Label label;

  /** constructor. */
  public VariableDeclarationNode(Variable variable, ImpType type, Label label) {
    this.variable = Objects.requireNonNull(variable);
    this.type = type;
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

  @Override
  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof VariableDeclarationNode)) {
      return false;
    }

    final VariableDeclarationNode that = (VariableDeclarationNode) o;
    return Objects.equals(this.variable, that.variable)
        && Objects.equals(this.type, that.type)
        && Objects.equals(this.label, that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.variable, this.type, this.label);
  }

  @Override
  public String toString() {
    return String.format("(varDecl %s as %s %s)", this.variable, this.type, this.label);
  }
}
