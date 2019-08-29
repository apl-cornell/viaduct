package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.informationflow.InformationFlowChecker;
import edu.cornell.cs.apl.viaduct.imp.informationflow.LabelTerm;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Objects;

/** A node in the abstract syntax tree of IMP programs. */
public abstract class ImpAstNode implements AstNode {
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
}
