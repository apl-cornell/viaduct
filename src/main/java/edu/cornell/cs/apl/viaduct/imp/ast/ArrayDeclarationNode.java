package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Objects;

public class ArrayDeclarationNode extends DeclarationNode {
  private final ExpressionNode length;

  public ArrayDeclarationNode(Variable variable, ExpressionNode length, Label label) {
    super(variable, label);
    this.length = length;
  }

  public ExpressionNode getLength() {
    return length;
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

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ArrayDeclarationNode that = (ArrayDeclarationNode) o;
    return super.equals(that) && this.length.equals(that.length);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.length);
  }

  @Override
  public String toString() {
    return "(arrayDeclaration "
        + this.getVariable()
        + "["
        + this.getLength()
        + "] as "
        + this.getLabel()
        + ")";
  }
}
