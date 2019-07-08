package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashSet;
import java.util.Set;

/** node in a program dependence graph. */
public abstract class PdgNode<T extends AstNode> {
  final T astNode;
  final String id;

  final Set<PdgInfoEdge<T>> inInfoEdges;
  final Set<PdgInfoEdge<T>> outInfoEdges;
  Label inLabel;
  Label outLabel;

  /** constructor. */
  public PdgNode(T astNode, String id) {
    this.inInfoEdges = new HashSet<>();
    this.outInfoEdges = new HashSet<>();
    this.inLabel = Label.weakestPrincipal();
    this.outLabel = Label.weakestPrincipal();
    this.astNode = astNode;
    this.id = id;
  }

  public T getAstNode() {
    return this.astNode;
  }

  public String getId() {
    return this.id;
  }

  public void addInInfoEdge(PdgInfoEdge<T> edge) {
    this.inInfoEdges.add(edge);
  }

  public void addOutInfoEdge(PdgInfoEdge<T> edge) {
    this.outInfoEdges.add(edge);
  }

  public Set<PdgInfoEdge<T>> getInInfoEdges() {
    return this.inInfoEdges;
  }

  /** return incoming read edges. */
  public Set<PdgReadEdge<T>> getReadEdges() {
    Set<PdgReadEdge<T>> readEdges = new HashSet<>();
    for (PdgInfoEdge<T> infoEdge : this.inInfoEdges) {
      if (infoEdge.isReadEdge()) {
        readEdges.add((PdgReadEdge<T>)infoEdge);
      }
    }

    return readEdges;
  }

  public Set<PdgInfoEdge<T>> getOutInfoEdges() {
    return this.outInfoEdges;
  }

  /** return outgoing write edges. */
  public Set<PdgWriteEdge<T>> getWriteEdges() {
    Set<PdgWriteEdge<T>> writeEdges = new HashSet<>();
    for (PdgInfoEdge<T> infoEdge : this.outInfoEdges) {
      if (infoEdge.isReadEdge()) {
        writeEdges.add((PdgWriteEdge<T>)infoEdge);
      }
    }

    return writeEdges;
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

  /** returns whether node marks off the beginning of a control fork.
   *  e.g. the first node in a then brach of a condition marks off
   * the beginning of a control fork. */
  public boolean isStartOfControlFork() {
    /*
    if (this.inControlEdge != null) {
      return this.inControlEdge.getLabel() != ControlLabel.SEQ;

    } else {
      return false;
    }
    */
    return false;
  }

  /** the node is the end of a execution path if it has
   * no outgoing control edges. */
  public boolean isEndOfExecutionPath() {
    /*
    boolean hasSeqOutControlEdge = false;
    for (PdgControlEdge<T> controlEdge : this.outControlEdges) {
      if (controlEdge.getLabel() == ControlLabel.SEQ) {
        hasSeqOutControlEdge = true;
        break;
      }
    }
    return !hasSeqOutControlEdge;
    */

    return false;
  }

  /** get the control node whose control structure this node resides in.
   * returns null if there isn't such a node */
  public PdgControlNode<T> getControlNode() {
    /*
    PdgNode<T> cur = this;

    while (cur != null) {
      if (cur.isControlNode() && cur != this) {
        return (PdgControlNode<T>)cur;

      } else {
        PdgControlEdge<T> controlEdge = cur.getInControlEdge();
        if (controlEdge != null) {
          cur = controlEdge.getSource();

        } else {
          cur = null;
        }
      }
    }
    */

    return null;
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

  @Override
  public String toString() {
    if (isDowngradeNode()) {
      return String.format("(pdg-node '%s' for '%s' with labels %s %s)",
          this.id, this.astNode, this.inLabel, this.outLabel);

    } else {
      return String.format("(pdg-node '%s' for '%s' with label %s)",
          this.id, this.astNode, this.outLabel);
    }
  }
}
