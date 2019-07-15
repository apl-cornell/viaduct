package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;

/** Process names. Processes execute code, and can send and receive messages. */
@AutoValue
public abstract class ProcessName implements Comparable<ProcessName> {
  private static final ProcessName MAIN = ProcessName.create("main");

  public static ProcessName create(String name) {
    return new AutoValue_ProcessName(name);
  }

  /** Get the default process name that corresponds to a host. */
  public static ProcessName create(Host host) {
    return create(host.getName());
  }

  /** Name of the entry process. */
  public static ProcessName getMain() {
    return MAIN;
  }

  public abstract String getName();

  @Override
  public final int compareTo(ProcessName that) {
    return getName().compareTo(that.getName());
  }

  @Override
  public final String toString() {
    return getName();
  }
}
