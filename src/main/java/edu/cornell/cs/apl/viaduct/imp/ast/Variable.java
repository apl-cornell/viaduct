package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import java.util.Objects;

/** A mutable variable that names a memory location. */
public final class Variable implements Reference, Binding<ImpAstNode> {
  private final String name;

  public <T extends AstNode> Variable(Binding<T> binding) {
    this.name = binding.getBinding();
  }

  public Variable(String name) {
    this.name = Objects.requireNonNull(name);
  }

  public String getName() {
    return this.name;
  }

  @Override
  public String getBinding() {
    return this.name;
  }

  @Override
  public <R> R accept(ReferenceVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Variable)) {
      return false;
    }

    final Variable that = (Variable) o;
    return Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  @Override
  public String toString() {
    return this.name;
  }
}
