package edu.cornell.cs.apl.viaduct.surface;

import edu.cornell.cs.apl.viaduct.Label;
import edu.cornell.cs.apl.viaduct.StmtVisitor;

/** Variable declaration. */
public class VarDeclNode implements StmtNode {
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
  public String toString() {
    return "(varDecl " + this.getVariable().toString() + " as " + this.getLabel().toString() + ")";
  }
}
