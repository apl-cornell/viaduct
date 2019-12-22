package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * represents dependencies (reads/writes) among part of a program. nodes represent declared
 * variables (storage) or computations (compute nodes) edge (A,B) represents a "read" of PDG node A
 * by PDG node B
 */
public class ProgramDependencyGraph<T extends AstNode> {
  public enum ControlLabel {
    SEQ,
    THEN,
    ELSE,
    BODY
  }

  List<PdgNode<T>> nodes;
  Map<String, PdgNode<T>> nodeMap;

  public ProgramDependencyGraph() {
    this.nodes = new ArrayList<>();
    this.nodeMap = new HashMap<>();
  }

  public void addNode(PdgNode<T> node) {
    this.nodes.add(node);
    this.nodeMap.put(node.getId(), node);
  }

  public PdgNode<T> getNode(String id) {
    return this.nodeMap.get(id);
  }

  /**
   * do a DFS traversal over control edges, which is almost equivalent to traversing the control
   * flow graph of the original program.
   */
  public List<PdgNode<T>> getOrderedNodesFrom(PdgNode<T> start) {
    /*
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
    */
    return null;
  }

  /** get PDG nodes ordered by control edges. */
  public List<PdgNode<T>> getOrderedNodes() {
    return this.nodes;
    /*
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
    */
  }

  @Override
  public String toString() {
    List<PdgNode<T>> sortedNodes = getOrderedNodes();

    StringBuffer buf = new StringBuffer();
    for (PdgNode<T> node : sortedNodes) {
      buf.append(node.toString());
      buf.append(" (outedges:");

      for (PdgInfoEdge<T> outEdge : node.getOutInfoEdges()) {
        buf.append(" " + outEdge.getTarget().getId());
      }
      buf.append(")\n");
    }

    return buf.toString();
  }
}
