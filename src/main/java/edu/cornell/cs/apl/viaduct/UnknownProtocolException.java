package edu.cornell.cs.apl.viaduct;

public class UnknownProtocolException extends RuntimeException {
  Protocol<? extends AstNode> protocol;

  public UnknownProtocolException(Protocol<? extends AstNode> proto) {
    this.protocol = proto;
  }

  public Protocol<? extends AstNode> getProtocol() {
    return this.protocol;
  }
}
