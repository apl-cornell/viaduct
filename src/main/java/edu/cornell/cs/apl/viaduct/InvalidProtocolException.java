package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;

public class InvalidProtocolException extends RuntimeException {
  PdgNode<? extends AstNode> node;
  Protocol<? extends AstNode> protocol;

  public InvalidProtocolException(
      PdgNode<? extends AstNode> node, Protocol<? extends AstNode> protocol) {
    this.node = node;
    this.protocol = protocol;
  }

  public PdgNode<? extends AstNode> getNode() {
    return this.node;
  }

  public Protocol<? extends AstNode> getProtocol() {
    return this.protocol;
  }
}
