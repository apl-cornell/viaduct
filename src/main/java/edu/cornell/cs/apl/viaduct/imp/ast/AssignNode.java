package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** Variable assignment statement. */
public final class AssignNode implements StmtNode {
  private final Variable variable;
  private final ExpressionNode rhs;

  public AssignNode(Variable var, ExpressionNode rhs) {
    this.variable = Objects.requireNonNull(var);
    this.rhs = Objects.requireNonNull(rhs);
  }

  public Variable getVariable() {
    return variable;
  }

  public ExpressionNode getRhs() {
    return rhs;
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

    if (!(o instanceof AssignNode)) {
      return false;
    }

    final AssignNode that = (AssignNode) o;
    return Objects.equals(this.variable, that.variable) && Objects.equals(this.rhs, that.rhs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.variable, this.rhs);
  }

  @Override
  public String toString() {
    return "(assign " + this.getVariable().toString() + " to " + this.getRhs().toString() + ")";
  }
}
