package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Label;

public interface AstNode {
  /**
   * Get the trust label associated with this node. This is the minimum trust the host executing
   * this node needs to have for the execution to be secure.
   */
  Label getTrustLabel();
}
