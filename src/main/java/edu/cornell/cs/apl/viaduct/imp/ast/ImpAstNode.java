package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.informationflow.InformationFlowChecker;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAstVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

/** A node in the abstract syntax tree of IMP programs. */
public abstract class ImpAstNode extends Located implements AstNode {
  private Supplier<Label> trustLabel;

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
    return trustLabel.get();
  }

  /**
   * Set the trust label of this node. This function is called by {@link InformationFlowChecker},
   * and should not be called manually.
   *
   * @throws IllegalArgumentException if called more than once
   */
  public final void setTrustLabel(@Nonnull Supplier<Label> value) throws IllegalArgumentException {
    if (trustLabel != null) {
      throw new IllegalArgumentException("Node label already set.");
    }
    trustLabel = Objects.requireNonNull(value);
  }

  public abstract <R> R accept(ImpAstVisitor<R> visitor);
}
