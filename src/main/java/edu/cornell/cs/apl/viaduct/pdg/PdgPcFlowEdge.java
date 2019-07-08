package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** represents information flow through change in PC label. */
public class PdgPcFlowEdge<T extends AstNode> extends PdgInfoEdge<T> {
  private PdgPcFlowEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  /** create edge b/w nodes. */
  public static <T extends AstNode> PdgPcFlowEdge<T> create(PdgNode<T> source, PdgNode<T> target) {
    PdgPcFlowEdge<T> flowEdge = new PdgPcFlowEdge<>(source, target);
    source.addOutInfoEdge(flowEdge);
    target.addInInfoEdge(flowEdge);
    return flowEdge;
  }

  @Override
  public boolean isPcFlowEdge() {
    return true;
  }
}
