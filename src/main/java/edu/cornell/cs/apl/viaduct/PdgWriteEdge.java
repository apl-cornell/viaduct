package edu.cornell.cs.apl.viaduct;

public class PdgWriteEdge<T extends AstNode> extends PdgInfoEdge<T> {
  public PdgWriteEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  /** create edge b/w nodes. */
  public static PdgWriteEdge create(PdgNode source, PdgNode target) {
    PdgWriteEdge writeEdge = new PdgWriteEdge(source, target);
    source.addOutInfoEdge(writeEdge);
    target.addInInfoEdge(writeEdge);
    return writeEdge;
  }
}
