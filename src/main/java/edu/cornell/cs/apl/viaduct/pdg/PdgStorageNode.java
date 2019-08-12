package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** PDG storage node, which represents declared variables. */
public class PdgStorageNode<T extends AstNode> extends PdgNode<T> {
  /** constructor. */
  public PdgStorageNode(T astNode, String id) {
    super(astNode, id);
  }

  @Override
  public boolean isStorageNode() {
    return true;
  }

  @Override
  public boolean isComputeNode() {
    return false;
  }

  @Override
  public boolean isControlNode() {
    return false;
  }

  @Override
  public String toString() {
    return "(" + this.id + " storage node for " + this.astNode.toString() + ")";
  }
}
