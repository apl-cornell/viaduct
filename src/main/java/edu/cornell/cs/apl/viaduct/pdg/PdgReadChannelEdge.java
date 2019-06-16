package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** represents information flow between nodes. no reads or writes. */
public class PdgReadChannelEdge<T extends AstNode> extends PdgInfoEdge<T> {
  public PdgReadChannelEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  @Override
  public boolean isReadChannelEdge() {
    return true;
  }

  /** create edge b/w nodes. */
  public static PdgReadChannelEdge create(PdgNode source, PdgNode target) {
    PdgReadChannelEdge flowEdge = new PdgReadChannelEdge(source, target);
    source.addOutInfoEdge(flowEdge);
    target.addInInfoEdge(flowEdge);
    return flowEdge;
  }
}
