package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Read the value of a variable. */
public class ReadNode implements ExpressionNode {
  private final Variable variable;

  public ReadNode(Variable variable) {
    this.variable = variable;
  }

  public Variable getVariable() {
    return variable;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(var " + this.getVariable().toString() + ")";
  }
}
