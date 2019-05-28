package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Read the value of a variable. */
public class ReadNode extends ExpressionNode {
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
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof ReadNode) {
      ReadNode otherRead = (ReadNode) other;
      return otherRead.variable.equals(this.variable);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.variable.hashCode();
  }

  @Override
  public String toString() {
    return "(var " + this.getVariable().toString() + ")";
  }
}
