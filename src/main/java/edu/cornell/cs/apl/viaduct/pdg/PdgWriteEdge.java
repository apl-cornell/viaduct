package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** represents writing to a variable. */
public class PdgWriteEdge<T extends AstNode> extends PdgInfoEdge<T> {
  private PdgWriteEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  /** create edge b/w nodes. */
  public static <T extends AstNode> PdgWriteEdge<T> create(PdgNode<T> source, PdgNode<T> target) {
    PdgWriteEdge<T> writeEdge = new PdgWriteEdge<>(source, target);
    source.addOutInfoEdge(writeEdge);
    target.addInInfoEdge(writeEdge);
    return writeEdge;
  }

  @Override
  public boolean isWriteEdge() {
    return true;
  }
}
