package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;

/** Process names. Processes execute code, and can send and receive messages. */
public final class ProcessName implements Comparable<ProcessName> {
  private static final ProcessName MAIN = new ProcessName("main");
  private final String name;

  public ProcessName(String name) {
    this.name = Objects.requireNonNull(name);
  }

  /** Get the default process name that corresponds to a host. */
  public ProcessName(Host host) {
    this(host.getName());
  }

  /** Name of the entry process. */
  public static ProcessName getMain() {
    return MAIN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ProcessName)) {
      return false;
    }

    final ProcessName that = (ProcessName) o;
    return Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name);
  }

  @Override
  public int compareTo(ProcessName o) {
    return this.name.compareTo(o.name);
  }

  @Override
  public String toString() {
    return this.name;
  }
}
