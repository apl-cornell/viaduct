package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import javax.annotation.Nonnull;

/** Process names. Processes execute code, and can send and receive messages. */
@AutoValue
public abstract class ProcessName extends Located implements Comparable<ProcessName>, Name {
  private static final ProcessName MAIN = ProcessName.create("main");

  // TODO: this shouldn't be here.
  private static final String freshProcessBaseName = "process";
  private static final FreshNameGenerator nameGenerator = new FreshNameGenerator();

  /** Name of the entry process. */
  public static ProcessName getMain() {
    return MAIN;
  }

  /** generate a fresh process name. */
  public static ProcessName createFreshName() {
    return create(nameGenerator.getFreshName(freshProcessBaseName));
  }

  public static ProcessName create(String name) {
    return builder().setName(name).setHost(false).build();
  }

  /** Get the default process name that corresponds to a host. */
  public static ProcessName create(HostName host) {
    return builder().setName(host.getName()).setHost(true).setSourceLocation(host).build();
  }

  public static Builder builder() {
    return new AutoValue_ProcessName.Builder().setHost(false);
  }

  // TODO: Ugh, don't do this.
  public abstract boolean isHost();

  public abstract Builder toBuilder();

  /** convert process name to host name. */
  public HostName toHostName() {
    if (isHost()) {
      return HostName.create(getName());

    } else {
      // TODO: use the correct exception type
      throw new Error(String.format("process %s is not a host", getName()));
    }
  }

  @Override
  public final String getNameCategory() {
    return "process";
  }

  @Override
  public int compareTo(@Nonnull ProcessName that) {
    return this.getName().compareTo(that.getName());
  }

  @Override
  public final String toString() {
    return getName();
  }

  @AutoValue.Builder
  public abstract static class Builder extends Located.Builder<Builder> {
    public abstract Builder setName(String name);

    public abstract Builder setHost(boolean isHost);

    public abstract ProcessName build();
  }
}
