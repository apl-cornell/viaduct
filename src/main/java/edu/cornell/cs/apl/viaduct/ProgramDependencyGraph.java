package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.AstNode;
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
    labelOrder.add(ControlLabel.SEQ);
    labelOrder.add(ControlLabel.ELSE);
    labelOrder.add(ControlLabel.THEN);
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

    // do a DFS traversal over control edges, which is
    // almost equivalent to traversing the control flow graph
    // of the original program
    ControlEdgeComparator edgeComparator = new ControlEdgeComparator();
    Stack<PdgNode<T>> rest = new Stack<>();
    rest.add(cur);

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

  class ControlEdgeComparator implements Comparator<PdgControlEdge<T>> {
    @Override
    public int compare(PdgControlEdge<T> e1, PdgControlEdge<T> e2) {
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
