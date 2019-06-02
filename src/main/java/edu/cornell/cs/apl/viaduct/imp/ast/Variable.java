package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.Binding;

/** A variable that can be read from or assigned to. */
public class Variable implements Binding<ImpAstNode> {
  private final String name;

  public <T extends ImpAstNode> Variable(Binding<T> binding) {
    this.name = binding.getBinding();
  }

  public Variable(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public String getBinding() {
    return this.name;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Variable) {
      Variable vo = (Variable) o;
      return this.name.equals(vo.getName());

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.name.hashCode();
  }

  @Override
  public String toString() {
    return this.name;
  }
}
