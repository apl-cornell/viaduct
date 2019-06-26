package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** Update the value associated with a reference. */
public final class AssignNode implements StmtNode {
  private final Reference lhs;
  private final ExpressionNode rhs;

  public AssignNode(Reference lhs, ExpressionNode rhs) {
    this.lhs = Objects.requireNonNull(lhs);
    this.rhs = Objects.requireNonNull(rhs);
  }

  public Reference getLhs() {
    return this.lhs;
  }

  public ExpressionNode getRhs() {
    return this.rhs;
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
    return Objects.equals(this.lhs, that.lhs) && Objects.equals(this.rhs, that.rhs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.lhs, this.rhs);
  }

  @Override
  public String toString() {
    return "(assign " + this.getLhs().toString() + " to " + this.getRhs().toString() + ")";
  }
}
