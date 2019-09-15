package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import java.util.Comparator;
import javax.annotation.Nonnull;

/** A location that can run (one or more) processes. */
@AutoValue
public abstract class HostName extends Located implements Comparable<HostName>, Name {
  public static HostName create(String name) {
    return create(name, false);
  }

  public static HostName create(String name, boolean isSynthetic) {
    return builder().setName(name).setSynthetic(isSynthetic).build();
  }

  public static Builder builder() {
    return new AutoValue_HostName.Builder().setSynthetic(false);
  }

  public abstract Builder toBuilder();

  // TODO: this will interact badly with .equals(). For example, compareTo is broken as is.
  public abstract boolean isSynthetic();

  @Override
  public final String getNameCategory() {
    return "host";
  }

  @Override
  public int compareTo(@Nonnull HostName that) {
    return Comparator.comparing(HostName::getName)
        .thenComparing(HostName::isSynthetic)
        .compare(this, that);
  }

  @Override
  public final String toString() {
    return getName();
  }

  @AutoValue.Builder
  public abstract static class Builder extends Located.Builder<Builder> {
    public abstract Builder setName(String name);

    public abstract Builder setSynthetic(boolean isSynthetic);

    public abstract HostName build();
  }
}
