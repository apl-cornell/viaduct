package edu.cornell.cs.apl.viaduct;

public abstract class PdgEdge<T extends AstNode> {
  PdgNode<T> source;
  PdgNode<T> target;

  public PdgEdge(PdgNode<T> s, PdgNode<T> t) {
    this.source = s;
    this.target = t;
  }

  public PdgNode<T> getSource() {
    return this.source;
  }

  public PdgNode<T> getTarget() {
    return this.target;
  }
}
