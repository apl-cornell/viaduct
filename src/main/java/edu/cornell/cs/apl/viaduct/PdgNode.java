package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.HashSet;
import java.util.Set;

/** node in a program dependence graph. */
public abstract class PdgNode<T extends AstNode> {
  T astNode;
  String id;

  Set<PdgInfoEdge<T>> inInfoEdges;
  PdgControlEdge<T> inControlEdge;
  Set<PdgInfoEdge<T>> outInfoEdges;
  Set<PdgControlEdge<T>> outControlEdges;
  Label inLabel;
  Label outLabel;

  protected PdgNode() {
    this.inInfoEdges = new HashSet<>();
    this.outInfoEdges = new HashSet<>();
    this.outControlEdges = new HashSet<>();
  }

  /** constructor. */
  public PdgNode(T astNode, String id) {
    this();
    this.astNode = astNode;
    this.id = id;
    this.inLabel = Label.bottom();
    this.outLabel = Label.bottom();
  }

  public T getAstNode() {
    return this.astNode;
  }

  public void setAstNode(T node) {
    this.astNode = node;
  }

  public String getId() {
    return this.id;
  }

  public void addInInfoEdge(PdgInfoEdge<T> edge) {
    this.inInfoEdges.add(edge);
  }

  public void setInControlEdge(PdgControlEdge<T> edge) {
    this.inControlEdge = edge;
  }

  public void addOutInfoEdge(PdgInfoEdge<T> edge) {
    this.outInfoEdges.add(edge);
  }

  public void addOutControlEdge(PdgControlEdge<T> edge) {
    this.outControlEdges.add(edge);
  }

  public Set<PdgInfoEdge<T>> getInInfoEdges() {
    return this.inInfoEdges;
  }

  public PdgControlEdge<T> getInControlEdge() {
    return this.inControlEdge;
  }

  public Set<PdgInfoEdge<T>> getOutInfoEdges() {
    return this.outInfoEdges;
  }

  public Set<PdgControlEdge<T>> getOutControlEdges() {
    return this.outControlEdges;
  }

  /**
   * returns all storage nodes that this PDG node transitively reads. this is used for compute nodes
   * for conditionals, where to model read channels the PC of the conditional has to be "written" to
   * storage nodes read in its branches.
   */
  public Set<PdgNode<T>> getStorageNodeInputs() {
    Set<PdgNode<T>> storageInputs = new HashSet<PdgNode<T>>();

    for (PdgInfoEdge<T> edge : this.inInfoEdges) {
      PdgNode<T> source = edge.getSource();
      if (source.isStorageNode()) {
        storageInputs.add(source);

      } else {
        storageInputs.addAll(source.getStorageNodeInputs());
      }
    }

    return storageInputs;
  }

  public Label getInLabel() {
    return this.inLabel;
  }

  public void setInLabel(Label label) {
    this.inLabel = label;
  }

  public Label getOutLabel() {
    return this.outLabel;
  }

  public void setOutLabel(Label label) {
    this.outLabel = label;
  }

  public boolean isEndorseNode() {
    return !this.inLabel.integrity().flowsTo(this.outLabel.integrity());
  }

  public boolean isDeclassifyNode() {
    return !this.inLabel.confidentiality().flowsTo(this.outLabel.confidentiality());
  }

  public abstract boolean isStorageNode();

  public abstract boolean isComputeNode();

  public abstract boolean isDowngradeNode();

  public abstract boolean isControlNode();

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof PdgNode<?>) {
      return this == other;
      /*
      PdgNode<T> otherPdg = (PdgNode<T>) other;
      return this.astNode.equals(otherPdg.astNode);
      */

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.astNode.hashCode();
  }
}
