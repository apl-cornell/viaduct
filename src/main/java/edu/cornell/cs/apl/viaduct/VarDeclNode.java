package edu.cornell.cs.apl.viaduct;

/** variable declaration. */
public class VarDeclNode implements StmtNode {
  Variable declaredVar;
  Label varLabel;

  public VarDeclNode(Variable var, Label label) {
    this.declaredVar = var;
    this.varLabel = label;
  }

  public Variable getDeclaredVar() {
    return this.declaredVar;
  }

  public Label getVarLabel() {
    return this.varLabel;
  }

  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(varDecl " + this.declaredVar.toString() + " as " + this.varLabel.toString() + ")";
  }
}
