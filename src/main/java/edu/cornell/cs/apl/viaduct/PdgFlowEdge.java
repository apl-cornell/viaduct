package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.AstNode;

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
