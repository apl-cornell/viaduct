package edu.cornell.cs.apl.viaduct.security;

import java.util.Objects;

/** An actor with an associated security label. */
public class Principal implements Comparable<Principal> {
  private final String name;

  public Principal(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Principal)) {
      return false;
    }

    final Principal that = (Principal) o;
    return Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name);
  }

  @Override
  public int compareTo(Principal o) {
    return this.name.compareTo(o.name);
  }

  @Override
  public String toString() {
    return this.name;
  }
}
