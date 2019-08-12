package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;

/** PDG compute node, which represents expressions or statements. */
public class PdgComputeNode<T extends AstNode> extends PdgNode<T> {
  /** constructor. */
  public PdgComputeNode(T astNode, String id) {
    super(astNode, id);
  }

  @Override
  public boolean isStorageNode() {
    return false;
  }

  @Override
  public boolean isComputeNode() {
    return true;
  }

  @Override
  public boolean isControlNode() {
    return false;
  }

  @Override
  public String toString() {
    return "(" + this.id + " compute node for " + this.astNode.toString() + ")";
  }
}
