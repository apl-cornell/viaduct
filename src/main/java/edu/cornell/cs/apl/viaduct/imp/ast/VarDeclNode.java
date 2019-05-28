package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Objects;

/** Variable declaration. */
public class VarDeclNode extends StmtNode {
  private final Variable variable;
  private final Label label;

  public VarDeclNode(Variable variable, Label label) {
    this.variable = variable;
    this.label = label;
  }

  public Variable getVariable() {
    return variable;
  }

  public Label getLabel() {
    return label;
  }

  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof VarDeclNode) {
      VarDeclNode otherDecl = (VarDeclNode) other;
      return otherDecl.variable.equals(this.variable) && otherDecl.label.equals(this.label);

    } else {
      return false;
    }
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
