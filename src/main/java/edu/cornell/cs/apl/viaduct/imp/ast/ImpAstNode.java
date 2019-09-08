package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.informationflow.InformationFlowChecker;
import edu.cornell.cs.apl.viaduct.imp.informationflow.LabelTerm;
import edu.cornell.cs.apl.viaduct.imp.parser.Located;
import edu.cornell.cs.apl.viaduct.imp.parser.SourceRange;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAstVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Objects;
import javax.annotation.Nullable;

/** A node in the abstract syntax tree of IMP programs. */
public abstract class ImpAstNode implements AstNode, Located {
  private LabelTerm trustLabel;

  /**
   * Get the trust label associated with this node. This is the minimum trust the host executing
   * this node needs to have for the execution to be secure.
   *
   * <p>The label is set by {@link InformationFlowChecker}. Calling this function before information
   * flow checking is done will result in an exception.
   *
   * @throws NullPointerException if called before label is set
   */
  @Override
  public final Label getTrustLabel() {
    return trustLabel.getValue();
  }

  /**
   * Set the trust label of this node. This function is called by {@link InformationFlowChecker},
   * and should not be called manually.
   *
   * @throws IllegalArgumentException if called more than once
   */
  public final void setTrustLabel(LabelTerm value) throws IllegalArgumentException {
    if (trustLabel != null) {
      throw new IllegalArgumentException("Node label already set.");
    }
    trustLabel = Objects.requireNonNull(value);
  }

  @Override
  public final @Nullable SourceRange getSourceLocation() {
    return getSourceLocationMetadata().getData();
  }

  /** Used internally to wrap/unwrap {@link Metadata}. */
  protected abstract Metadata<SourceRange> getSourceLocationMetadata();

  public abstract <R> R accept(ImpAstVisitor<R> visitor);

  protected abstract static class Builder<SelfT> {
    public final SelfT setSourceLocation(@Nullable SourceRange sourceLocation) {
      return setSourceLocationMetadata(new Metadata<>(sourceLocation));
    }

    public final SelfT setSourceLocation(Located node) {
      return setSourceLocation(node.getSourceLocation());
    }

    /**
     * Set fields with defaults to their default values.
     *
     * <p>NOTE: Unfortunately, this needs to be called manually by every subclass.
     */
    protected final SelfT setDefaults() {
      return setSourceLocationMetadata(new Metadata<>(null));
    }

    /** Used internally to wrap/unwrap {@link Metadata}. */
    protected abstract SelfT setSourceLocationMetadata(
        Metadata<SourceRange> sourceLocationMetadata);
  }
}
