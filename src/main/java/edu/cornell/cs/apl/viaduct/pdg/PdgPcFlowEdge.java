package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** represents information flow through change in PC label. */
public class PdgPcFlowEdge<T extends AstNode> extends PdgInfoEdge<T> {
  public PdgPcFlowEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  @Override
  public boolean isPcFlowEdge() {
    return true;
  }

  /** create edge b/w nodes. */
  public static PdgPcFlowEdge create(PdgNode source, PdgNode target) {
    PdgPcFlowEdge flowEdge = new PdgPcFlowEdge(source, target);
    source.addOutInfoEdge(flowEdge);
    target.addInInfoEdge(flowEdge);
    return flowEdge;
  }
}
