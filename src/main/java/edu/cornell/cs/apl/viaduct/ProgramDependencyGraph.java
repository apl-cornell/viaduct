package edu.cornell.cs.apl.viaduct;

import java.util.ArrayList;
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

  /** get PDG nodes ordered by control edges. */
  public List<PdgNode<T>> getOrderedNodes() {
    List<PdgNode<T>> nodeList = new ArrayList<>();
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
    nodeList.add(cur);

    Stack<PdgNode<T>> rest = new Stack<>();
    for (PdgControlEdge<T> ctrlEdge : cur.getOutControlEdges()) {
      rest.add(ctrlEdge.getTarget());
    }

    while (!rest.isEmpty()) {
      cur = rest.pop();
      nodeList.add(cur);
      for (PdgControlEdge<T> ctrlEdge : cur.getOutControlEdges()) {
        rest.add(ctrlEdge.getTarget());
      }
    }

    return nodeList;
  }

  @Override
  public String toString() {
    List<PdgNode<T>> sortedNodes = getOrderedNodes();

    StringBuffer buf = new StringBuffer();
    for (PdgNode<T> node : sortedNodes) {
      buf.append(node.toString());
      buf.append(" (outedges:");

      for (PdgNode<T> outNode : node.getOutNodes()) {
        buf.append(" " + outNode.getId().toString());
      }
      buf.append(")\n");
    }

    return buf.toString();
  }
}
