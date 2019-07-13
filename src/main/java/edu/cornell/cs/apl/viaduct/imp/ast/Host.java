package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;

/** A location that can run (one or more) processes. */
@AutoValue
public abstract class Host implements Comparable<Host> {
  public static Host create(String name) {
    return new AutoValue_Host(name);
  }

  public abstract String getName();

  @Override
  public final int compareTo(Host that) {
    return this.getName().compareTo(that.getName());
  }

  @Override
  public final String toString() {
    return getName();
  }
}
