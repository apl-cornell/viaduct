package edu.cornell.cs.apl.viaduct;

public abstract class PdgInfoEdge<T extends AstNode> extends PdgEdge<T> {
  public PdgInfoEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }
}
