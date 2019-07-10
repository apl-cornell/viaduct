package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** a read from a storage node. */
public final class PdgQueryEdge<T extends AstNode> extends PdgReadEdge<T> {
  private final T query;

  private PdgQueryEdge(PdgNode<T> source, PdgNode<T> target, T query) {
    super(source, target);
    this.query = query;
  }

  /** create edge b/w nodes. */
  public static <T extends AstNode> PdgQueryEdge<T> create(
      PdgNode<T> source, PdgNode<T> target, T query) {

    PdgQueryEdge<T> readEdge = new PdgQueryEdge<>(source, target, query);
    source.addOutInfoEdge(readEdge);
    target.addInInfoEdge(readEdge);
    return readEdge;
  }

  public T getQuery() {
    return this.query;
  }

  @Override
  public boolean isQueryEdge() {
    return true;
  }

  @Override
  public boolean isComputeEdge() {
    return false;
  }

  @Override
  public String getLabel() {
    return this.query.toString();
  }
}
