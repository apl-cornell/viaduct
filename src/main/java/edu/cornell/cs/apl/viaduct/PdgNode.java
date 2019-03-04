package edu.cornell.cs.apl.viaduct;

import java.util.HashSet;
import java.util.Set;

/** node in a program dependence graph. */
public abstract class PdgNode {
  AstNode astNode;
  Set<PdgNode> inNodes;
  Set<PdgNode> outNodes;

  /** constructor. */
  public PdgNode(AstNode astNode, Set<PdgNode> inNodes, Set<PdgNode> outNodes) {
    this.inNodes = inNodes;
    this.outNodes = outNodes;
    this.astNode = astNode;
  }

  /** constructor. */
  public PdgNode(AstNode astNode) {
    this(astNode, new HashSet<PdgNode>(), new HashSet<PdgNode>());
  }

  public void addInNode(PdgNode node) {
    this.inNodes.add(node);
  }

  public void addInNodes(Set<PdgNode> nodes) {
    this.inNodes.addAll(nodes);
  }

  public void addOutNode(PdgNode node) {
    this.outNodes.add(node);
  }

  public void addOutNodes(Set<PdgNode> nodes) {
    this.outNodes.addAll(nodes);
  }

  public Set<PdgNode> getInNodes() {
    return this.inNodes;
  }

  public Set<PdgNode> getOutNodes() {
    return this.outNodes;
  }

  /**
   * returns all storage nodes that this PDG node transitively reads. this is used for compute nodes
   * for conditionals, where to model read channels the PC of the conditional has to be "written" to
   * storage nodes read in its branches.
   */
  public Set<PdgNode> getStorageNodeInputs() {
    Set<PdgNode> storageInputs = new HashSet<PdgNode>();
    for (PdgNode inNode : this.inNodes) {
      if (inNode.isStorageNode()) {
        storageInputs.add(inNode);

      } else {
        storageInputs.addAll(inNode.getStorageNodeInputs());
      }
    }

    return storageInputs;
  }

  public abstract Label getLabel();

  public abstract void setLabel(Label label);

  public Label getInLabel() {
    return this.getLabel();
  }

  public void setInLabel(Label label) {
    this.setLabel(label);
  }

  public Label getOutLabel() {
    return this.getLabel();
  }

  public void setOutLabel(Label label) {
    this.setLabel(label);
  }

  public abstract boolean isStorageNode();

  public abstract boolean isComputeNode();

  public abstract boolean isDowngradeNode();
}
