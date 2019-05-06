package edu.cornell.cs.apl.viaduct;

import java.util.HashSet;
import java.util.Set;

/** node in a program dependence graph. */
public abstract class PdgNode<T extends AstNode> implements Comparable<PdgNode<T>> {
  T astNode;
  AbstractLineNumber lineNumber;
  Set<PdgNode<T>> inNodes;
  Set<PdgNode<T>> outNodes;
  Label inLabel;
  Label outLabel;

  /** constructor. */
  public PdgNode(
      T astNode, AbstractLineNumber lineno, Set<PdgNode<T>> inNodes, Set<PdgNode<T>> outNodes) {

    this.astNode = astNode;
    this.lineNumber = lineno;
    this.inNodes = inNodes;
    this.outNodes = outNodes;
    this.inLabel = Label.bottom();
    this.outLabel = Label.bottom();
  }

  /** constructor, defaults to no edges. */
  public PdgNode(T astNode, AbstractLineNumber lineno) {
    this(astNode, lineno, new HashSet<PdgNode<T>>(), new HashSet<PdgNode<T>>());
  }

  public T getAstNode() {
    return this.astNode;
  }

  public void setAstNode(T node) {
    this.astNode = node;
  }

  public AbstractLineNumber getLineNumber() {
    return this.lineNumber;
  }

  public void setLineNumber(AbstractLineNumber lineno) {
    this.lineNumber = lineno;
  }

  public void addInNode(PdgNode<T> node) {
    this.inNodes.add(node);
  }

  public void addInNodes(Set<PdgNode<T>> nodes) {
    this.inNodes.addAll(nodes);
  }

  public void addOutNode(PdgNode<T> node) {
    this.outNodes.add(node);
  }

  public void addOutNodes(Set<PdgNode<T>> nodes) {
    this.outNodes.addAll(nodes);
  }

  public Set<PdgNode<T>> getInNodes() {
    return this.inNodes;
  }

  public Set<PdgNode<T>> getOutNodes() {
    return this.outNodes;
  }

  /**
   * returns all storage nodes that this PDG node transitively reads. this is used for compute nodes
   * for conditionals, where to model read channels the PC of the conditional has to be "written" to
   * storage nodes read in its branches.
   */
  public Set<PdgNode<T>> getStorageNodeInputs() {
    Set<PdgNode<T>> storageInputs = new HashSet<PdgNode<T>>();
    for (PdgNode<T> inNode : this.inNodes) {
      if (inNode.isStorageNode()) {
        storageInputs.add(inNode);

      } else {
        storageInputs.addAll(inNode.getStorageNodeInputs());
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

  public int compareTo(PdgNode<T> other) {
    return this.lineNumber.compareTo(other.lineNumber);
  }

  /*
  @Override
  public boolean equals(Object o) {
    if (o instanceof PdgNode<?>) {
      PdgNode<T> otherPdg = (PdgNode<T>)o;
      return this.astNode.equals(otherPdg.astNode);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.astNode.hashCode();
  }
  */
}
