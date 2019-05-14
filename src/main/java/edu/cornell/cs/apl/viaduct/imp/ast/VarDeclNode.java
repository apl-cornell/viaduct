package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;

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
  public String toString() {
    return "(varDecl " + this.getVariable().toString() + " as " + this.getLabel().toString() + ")";
  }
}
