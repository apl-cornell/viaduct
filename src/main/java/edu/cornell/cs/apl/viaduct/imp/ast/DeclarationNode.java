package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Objects;

/** Variable declaration. */
public final class DeclarationNode extends StmtNode {
  private final Variable variable;
  private final Label label;

  public DeclarationNode(Variable variable, Label label) {
    this.variable = Objects.requireNonNull(variable);
    this.label = Objects.requireNonNull(label);
  }

  public Variable getVariable() {
    return variable;
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

    if (!(o instanceof DeclarationNode)) {
      return false;
    }

    final DeclarationNode that = (DeclarationNode) o;
    return Objects.equals(this.variable, that.variable) && Objects.equals(this.label, that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.variable, this.label);
  }

  @Override
  public String toString() {
    return "(varDecl " + this.getVariable().toString() + " as " + this.getLabel().toString() + ")";
  }
}
