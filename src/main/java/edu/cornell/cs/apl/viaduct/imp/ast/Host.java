package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;

/** An abstract location or actor that can send and receive messages. */
public final class Host implements Comparable<Host> {
  private final String name;

  public Host(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Host that = (Host) o;
    return this.name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name);
  }

  @Override
  public int compareTo(Host o) {
    return this.name.compareTo(o.name);
  }

  @Override
  public String toString() {
    return this.name;
  }
}
