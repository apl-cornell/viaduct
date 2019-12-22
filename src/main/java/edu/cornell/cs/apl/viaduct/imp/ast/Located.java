package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange;
import edu.cornell.cs.apl.viaduct.util.AbstractLineNumber;
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

  @Override
  public final AbstractLineNumber getLogicalPosition() {
    return getLogicalPositionMetadata().getData();
  }

  /** Used internally to wrap/unwrap {@link Metadata}. */
  protected abstract Metadata<SourceRange> getSourceLocationMetadata();

  /** Used internally to wrap/unwrap {@link Metadata}. */
  protected abstract Metadata<AbstractLineNumber> getLogicalPositionMetadata();

  protected abstract static class Builder<SelfT> {
    protected Builder() {
      setSourceLocationMetadata(new Metadata<>(null));
      setLogicalPositionMetadata(new Metadata<>(null));
    }

    public final SelfT setSourceLocation(@Nullable SourceRange sourceLocation) {
      return setSourceLocationMetadata(new Metadata<>(sourceLocation));
    }

    public final SelfT setSourceLocation(HasLocation node) {
      return setSourceLocation(node.getSourceLocation());
    }

    public final SelfT setLogicalPosition(@Nullable AbstractLineNumber lineNo) {
      return setLogicalPositionMetadata(new Metadata<>(lineNo));
    }

    public final SelfT setLogicalPosition(HasLocation node) {
      return setLogicalPosition(node.getLogicalPosition());
    }

    public final SelfT setLocation(HasLocation node) {
      setSourceLocation(node.getSourceLocation());
      return setLogicalPosition(node.getLogicalPosition());
    }

    /** Used internally to wrap/unwrap {@link Metadata}. */
    protected abstract SelfT setSourceLocationMetadata(
        Metadata<SourceRange> sourceLocationMetadata);

    /** Used internally to wrap/unwrap {@link Metadata}. */
    protected abstract SelfT setLogicalPositionMetadata(
        Metadata<AbstractLineNumber> abstractLineNumberMetadata);
  }
}
