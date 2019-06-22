package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;

public class ArrayIndexValue implements ImpLValue {
  Variable variable;
  int index;

  public ArrayIndexValue(Variable v, int i) {
    this.variable = v;
    this.index = i;
  }

  public Variable getVariable() {
    return this.variable;
  }

  public int getIndex() {
    return this.index;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ArrayIndexValue)) {
      return false;
    }

    final ArrayIndexValue that = (ArrayIndexValue) o;
    return Objects.equals(this.variable, that.variable) && Objects.equals(this.index, that.index);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.variable, this.index);
  }

  @Override
  public String toString() {
    return String.format("%s[%d]", this.variable.toString(), this.index);
  }
}
