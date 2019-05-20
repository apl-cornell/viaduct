package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  @Override
  public String toString() {
    List<PdgNode<T>> sortedNodes = new ArrayList<PdgNode<T>>(this.nodes);
    Collections.sort(sortedNodes);

    StringBuffer buf = new StringBuffer();
    for (PdgNode<T> node : sortedNodes) {
      buf.append(node.toString());
      buf.append(" (outedges:");

      for (PdgNode<T> outNode : node.getOutNodes()) {
        buf.append(" " + outNode.getLineNumber().toString());
      }
      buf.append(")\n");
    }

    return buf.toString();
  }
}
