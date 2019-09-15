package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;

/** Target labels for unstructured control statements like {@code continue} and {@code break}. */
@AutoValue
public abstract class JumpLabel extends Located implements Name {
  public static Builder builder() {
    return new AutoValue_JumpLabel.Builder();
  }

  public abstract Builder toBuilder();

  @Override
  public final String getNameCategory() {
    return "label";
  }

  @Override
  public final String toString() {
    return getName();
  }

  @AutoValue.Builder
  public abstract static class Builder extends Located.Builder<Builder> {
    public abstract Builder setName(String name);

    public abstract JumpLabel build();
  }
}
