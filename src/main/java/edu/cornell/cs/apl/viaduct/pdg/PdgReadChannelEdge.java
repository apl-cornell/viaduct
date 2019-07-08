package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** represents information flow between nodes. no reads or writes. */
public class PdgReadChannelEdge<T extends AstNode> extends PdgInfoEdge<T> {
  private PdgReadChannelEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  /** create edge b/w nodes. */
  public static <T extends AstNode> PdgReadChannelEdge<T> create(
      PdgNode<T> source, PdgNode<T> target) {
    PdgReadChannelEdge<T> flowEdge = new PdgReadChannelEdge<>(source, target);
    source.addOutInfoEdge(flowEdge);
    target.addInInfoEdge(flowEdge);
    return flowEdge;
  }

  @Override
  public boolean isReadChannelEdge() {
    return true;
  }
}
