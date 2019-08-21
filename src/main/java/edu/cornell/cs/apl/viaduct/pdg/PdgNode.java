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

  /** constructor. */
  public PdgNode(T astNode, String id) {
    this.inInfoEdges = new HashSet<>();
    this.outInfoEdges = new HashSet<>();
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
        readEdges.add((PdgReadEdge<T>) infoEdge);
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
      if (infoEdge.isWriteEdge()) {
        writeEdges.add((PdgWriteEdge<T>) infoEdge);
      }
    }

    return writeEdges;
  }

  /** return all info edges. */
  public Set<PdgInfoEdge<T>> getInfoEdges() {
    Set<PdgInfoEdge<T>> infoEdges = new HashSet<>();
    infoEdges.addAll(this.inInfoEdges);
    infoEdges.addAll(this.outInfoEdges);
    return infoEdges;
  }

  /**
   * returns all storage nodes that this PDG node transitively reads. this is used for compute nodes
   * for conditionals, where to model read channels the PC of the conditional has to be "written" to
   * storage nodes read in its branches.
   */
  public Set<PdgNode<T>> getStorageNodeInputs() {
    Set<PdgNode<T>> storageInputs = new HashSet<PdgNode<T>>();

    for (PdgInfoEdge<T> edge : this.inInfoEdges) {
      if (edge.isReadChannelEdge()) {
        PdgNode<T> source = edge.getSource();
        if (source.isStorageNode()) {
          storageInputs.add(source);

        } else {
          storageInputs.addAll(source.getStorageNodeInputs());
        }
      }
    }

    return storageInputs;
  }

  /** get all the nodes (transitively) read by this node. */
  public Set<PdgNode<T>> getReadNodes() {
    Set<PdgNode<T>> readNodes = new HashSet<>();

    for (PdgInfoEdge<T> edge : this.inInfoEdges) {
      if (edge.isReadEdge()) {
        PdgNode<T> source = edge.getSource();
        readNodes.add(source);
        readNodes.addAll(source.getReadNodes());
      }
    }

    return readNodes;
  }

  /** Get label. */
  public Label getLabel() {
    try {
      return this.astNode.getTrustLabel();
    } catch (NullPointerException e) {
      return null;
    }
  }

  public abstract boolean isStorageNode();

  public abstract boolean isComputeNode();

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
    return String.format(
        "(pdg-node '%s' for '%s' with label %s)", this.id, this.astNode, getLabel());
  }
}
