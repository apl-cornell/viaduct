package edu.cornell.cs.apl.viaduct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * represents dependencies (reads/writes) among part of a program. nodes represent declared
 * variables (storage) or computations (compute nodes) edge (A,B) represents a "read" of PDG node A
 * by PDG node B
 */
public class ProgramDependencyGraph<T extends AstNode> {
  static final List<ControlLabel> labelOrder;

  static {
    labelOrder = new ArrayList<>();
    labelOrder.add(ControlLabel.THEN);
    labelOrder.add(ControlLabel.ELSE);
    labelOrder.add(ControlLabel.SEQ);
  }

  HashSet<PdgNode<T>> nodes;

  public ProgramDependencyGraph() {
    this.nodes = new HashSet<PdgNode<T>>();
  }

  public void addNode(PdgNode<T> node) {
    this.nodes.add(node);
  }

  public Set<PdgNode<T>> getNodes() {
    return this.nodes;
  }

  /**
   * do a DFS traversal over control edges, which is almost equivalent to traversing the control
   * flow graph of the original program.
   */
  public List<PdgNode<T>> getOrderedNodesFrom(PdgNode<T> start) {
    ControlEdgeComparator edgeComparator = new ControlEdgeComparator();

    PdgNode<T> cur = start;
    Stack<PdgNode<T>> rest = new Stack<>();
    rest.add(cur);

    List<PdgNode<T>> nodeList = new ArrayList<>();
    while (!rest.isEmpty()) {
      cur = rest.pop();
      nodeList.add(cur);
      Set<PdgControlEdge<T>> edgeSet = cur.getOutControlEdges();
      List<PdgControlEdge<T>> orderedEdges = new ArrayList<>(edgeSet);
      Collections.sort(orderedEdges, edgeComparator);
      Collections.reverse(orderedEdges);
      for (PdgControlEdge<T> ctrlEdge : orderedEdges) {
        rest.add(ctrlEdge.getTarget());
      }
    }

    return nodeList;
  }

  /** get PDG nodes ordered by control edges. */
  public List<PdgNode<T>> getOrderedNodes() {
    PdgNode<T> cur = null;

    // find first node, which is the node that has no
    // incoming control edge (root of AST)
    for (PdgNode<T> node : nodes) {
      if (node.getInControlEdge() == null) {
        cur = node;
        break;
      }
    }
    assert cur != null;

    return getOrderedNodesFrom(cur);
  }

  @Override
  public String toString() {
    List<PdgNode<T>> sortedNodes = getOrderedNodes();

    StringBuffer buf = new StringBuffer();
    for (PdgNode<T> node : sortedNodes) {
      buf.append(node.toString());
      buf.append(" (outedges:");

      for (PdgInfoEdge<T> outEdge : node.getOutInfoEdges()) {
        buf.append(" " + outEdge.getTarget().getId().toString());
      }
      buf.append(")\n");
    }

    return buf.toString();
  }

  public enum ControlLabel {
    SEQ,
    THEN,
    ELSE
  }

  static class ControlEdgeComparator implements Comparator<PdgControlEdge>, Serializable {
    @Override
    public int compare(PdgControlEdge e1, PdgControlEdge e2) {
      if (e1 != null && e2 != null) {
        int ind1 = labelOrder.indexOf(e1.getLabel());
        int ind2 = labelOrder.indexOf(e2.getLabel());
        return ind1 - ind2;

      } else if (e1 == null && e2 != null) {
        return 1;

      } else if (e1 != null && e2 == null) {
        return -1;

      } else {
        return 0;
      }
    }
  }
}
