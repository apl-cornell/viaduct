package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** represents reading from a variable or computation. */
public abstract class PdgReadEdge<T extends AstNode> extends PdgInfoEdge<T> {
  protected PdgReadEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  public abstract boolean isQueryEdge();

  public abstract boolean isComputeEdge();

  @Override
  public boolean isReadEdge() {
    return true;
  }
}
