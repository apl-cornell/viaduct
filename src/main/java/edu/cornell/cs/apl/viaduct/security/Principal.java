package edu.cornell.cs.apl.viaduct.security;

import com.google.auto.value.AutoValue;

/** An actor with an associated security label. */
@AutoValue
public abstract class Principal implements Comparable<Principal> {
  public static Principal create(String name) {
    return new AutoValue_Principal(name);
  }

  abstract String getName();

  @Override
  public final int compareTo(Principal that) {
    return this.getName().compareTo(that.getName());
  }

  @Override
  public final String toString() {
    return getName();
  }
}
