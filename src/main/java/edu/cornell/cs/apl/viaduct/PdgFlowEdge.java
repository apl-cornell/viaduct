package edu.cornell.cs.apl.viaduct;

/** represents information flow between nodes. no reads or writes. */
public class PdgFlowEdge<T extends AstNode> extends PdgInfoEdge<T> {
  public PdgFlowEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  @Override
  public boolean isFlowEdge() {
    return true;
  }
}
