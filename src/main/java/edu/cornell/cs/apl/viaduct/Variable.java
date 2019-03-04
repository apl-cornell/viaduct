package edu.cornell.cs.apl.viaduct;

/** represents referenced or declared variables. */
public class Variable {
  String name;

  public Variable(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public String toString() {
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
}
