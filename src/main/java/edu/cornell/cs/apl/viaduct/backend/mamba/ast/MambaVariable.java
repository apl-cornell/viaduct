package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MambaVariable implements MambaAstNode {
  public static MambaVariable create(String name) {
    return builder().setName(name).build();
  }

  public static Builder builder() {
    return new AutoValue_MambaVariable.Builder();
  }

  public abstract Builder toBuilder();

  public abstract String getName();

  @Override
  public final String toString() {
    return getName();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);

    public abstract MambaVariable build();
  }
}
