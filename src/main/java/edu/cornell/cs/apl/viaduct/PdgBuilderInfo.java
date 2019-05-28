package edu.cornell.cs.apl.viaduct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** track information during building a PDG. */
public class PdgBuilderInfo<T extends AstNode> {
  Set<PdgNode<T>> referencedNodes;
  Set<PdgNode<T>> createdNodes;
  Map<PdgNode<T>, Binding<T>> refBindingMap;
  PdgNode<T> firstCreated;
  PdgNode<T> lastCreated;

  /** constructor. */
  public PdgBuilderInfo() {
    this.referencedNodes = new HashSet<>();
    this.createdNodes = new HashSet<>();
    this.refBindingMap = new HashMap<>();
  }

  public PdgBuilderInfo(PdgNode<T> createdNode) {
    this();
    addCreatedNode(createdNode);
  }

  public PdgBuilderInfo(PdgNode<T> createdNode, Binding<T> binding) {
    this();
    addCreatedNode(createdNode, binding);
  }

  protected PdgBuilderInfo(Set<PdgNode<T>> rnodes, Set<PdgNode<T>> cnodes) {

    this();
    this.referencedNodes = rnodes;
    this.createdNodes = cnodes;
  }

  public void addReferencedNode(PdgNode<T> refNode, Binding<T> binding) {
    this.referencedNodes.add(refNode);
    this.refBindingMap.put(refNode, binding);
  }

  /** add a created node. */
  public void addCreatedNode(PdgNode<T> createdNode) {
    this.createdNodes.add(createdNode);

    if (this.firstCreated == null) {
      this.firstCreated = createdNode;
    }

    this.lastCreated = createdNode;
  }

  public void addCreatedNode(PdgNode<T> createdNode, Binding<T> binding) {
    addCreatedNode(createdNode);
    this.refBindingMap.put(createdNode, binding);
  }

  /** merge two PDG builder infos togther. */
  public PdgBuilderInfo<T> merge(PdgBuilderInfo<T> other) {
    Set<PdgNode<T>> rnodes = new HashSet<>();
    rnodes.addAll(this.referencedNodes);
    rnodes.addAll(other.referencedNodes);

    Set<PdgNode<T>> cnodes = new HashSet<>();
    cnodes.addAll(this.createdNodes);
    cnodes.addAll(other.createdNodes);

    PdgBuilderInfo<T> mergedInfo = new PdgBuilderInfo<>(rnodes, cnodes);
    mergedInfo.firstCreated = this.firstCreated;
    mergedInfo.lastCreated = this.lastCreated;
    mergedInfo.refBindingMap.putAll(this.refBindingMap);
    mergedInfo.refBindingMap.putAll(other.refBindingMap);

    if (mergedInfo.firstCreated == null) {
      mergedInfo.firstCreated = other.firstCreated;
    }

    if (other.lastCreated != null) {
      mergedInfo.lastCreated = other.lastCreated;
    }

    return mergedInfo;
  }

  /** merge only created nodes. */
  public PdgBuilderInfo<T> mergeCreated(PdgBuilderInfo<T> other) {
    Set<PdgNode<T>> cnodes = new HashSet<>();
    cnodes.addAll(this.createdNodes);
    cnodes.addAll(other.createdNodes);

    PdgBuilderInfo<T> mergedInfo = new PdgBuilderInfo<>(this.referencedNodes, cnodes);
    mergedInfo.firstCreated = this.firstCreated;
    mergedInfo.lastCreated = this.lastCreated;
    mergedInfo.refBindingMap.putAll(this.refBindingMap);
    mergedInfo.refBindingMap.putAll(other.refBindingMap);

    if (mergedInfo.firstCreated == null) {
      mergedInfo.firstCreated = other.firstCreated;
    }

    if (other.lastCreated != null) {
      mergedInfo.lastCreated = other.lastCreated;
    }

    return mergedInfo;
  }

  public Set<PdgNode<T>> getReferencedNodes() {
    return this.referencedNodes;
  }

  public Binding<T> getReferenceBinding(PdgNode<T> refNode) {
    return this.refBindingMap.get(refNode);
  }

  public Set<PdgNode<T>> getCreatedNodes() {
    return this.createdNodes;
  }

  /** get both referenced and created nodes. */
  public Set<PdgNode<T>> getAllNodes() {
    Set<PdgNode<T>> allNodes = new HashSet<>();
    allNodes.addAll(this.referencedNodes);
    allNodes.addAll(this.createdNodes);
    return allNodes;
  }

  public PdgNode<T> getFirstCreated() {
    return this.firstCreated;
  }

  public PdgNode<T> getLastCreated() {
    return this.lastCreated;
  }

  /** create edges for a node reading the current info. */
  public void setReadNode(PdgNode<T> node) {
    for (PdgNode<T> refNode : this.referencedNodes) {
      Binding<T> binding = this.refBindingMap.get(refNode);
      PdgReadEdge.create(refNode, node, binding);
    }

    for (PdgNode<T> createdNode : this.createdNodes) {
      Binding<T> binding = this.refBindingMap.get(createdNode);

      if (binding != null) {
        PdgReadEdge.create(createdNode, node, binding);

      } else {
        PdgReadEdge.create(createdNode, node);
      }
    }

    if (this.lastCreated != null) {
      PdgControlEdge.create(this.lastCreated, node);
    }
  }
}
