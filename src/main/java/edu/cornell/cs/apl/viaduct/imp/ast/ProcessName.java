package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;

/** Process names. Processes execute code, and can send and receive messages. */
@AutoValue
public abstract class ProcessName extends Located implements Name {
  private static final ProcessName MAIN = ProcessName.create("main");

  /** Name of the entry process. */
  public static ProcessName getMain() {
    return MAIN;
  }

  public static ProcessName create(String name) {
    return builder().setName(name).build();
  }

  /** Get the default process name that corresponds to a host. */
  public static ProcessName create(HostName host) {
    return builder().setName(host.getName()).setSourceLocation(host).build();
  }

  public static Builder builder() {
    return new AutoValue_ProcessName.Builder();
  }

  public abstract Builder toBuilder();

  @Override
  public final String getNameCategory() {
    return "process";
  }

  @Override
  public final String toString() {
    return getName();
  }

  @AutoValue.Builder
  public abstract static class Builder extends Located.Builder<Builder> {
    public abstract Builder setName(String name);

    public abstract ProcessName build();
  }
}
