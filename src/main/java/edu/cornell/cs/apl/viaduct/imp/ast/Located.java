package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange;
import javax.annotation.Nullable;

/**
 * A class that maintains a source location. This class is meant to be a "mixin" for subclasses that
 * use AutoValue.
 */
abstract class Located implements HasLocation {
  @Override
  public final SourceRange getSourceLocation() {
    return getSourceLocationMetadata().getData();
  }

  /** Used internally to wrap/unwrap {@link Metadata}. */
  protected abstract Metadata<SourceRange> getSourceLocationMetadata();

  protected abstract static class Builder<SelfT> {
    protected Builder() {
      setSourceLocationMetadata(new Metadata<>(null));
    }

    public final SelfT setSourceLocation(@Nullable SourceRange sourceLocation) {
      return setSourceLocationMetadata(new Metadata<>(sourceLocation));
    }

    public final SelfT setSourceLocation(HasLocation node) {
      return setSourceLocation(node.getSourceLocation());
    }

    /** Used internally to wrap/unwrap {@link Metadata}. */
    protected abstract SelfT setSourceLocationMetadata(
        Metadata<SourceRange> sourceLocationMetadata);
  }
}
