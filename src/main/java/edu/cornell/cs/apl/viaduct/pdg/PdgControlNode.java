package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** PDG node for control flow statements. */
public class PdgControlNode<T extends AstNode> extends PdgNode<T> {
  /** constructor. */
  public PdgControlNode(T astNode, String id) {
    super(astNode, id);
  }

  @Override
  public boolean isStorageNode() {
    return false;
  }

  @Override
  public boolean isComputeNode() {
    return false;
  }

  @Override
  public boolean isControlNode() {
    return true;
  }

  @Override
  public String toString() {
    return "(" + this.id + " control node for " + this.astNode.toString() + ")";
  }
}
