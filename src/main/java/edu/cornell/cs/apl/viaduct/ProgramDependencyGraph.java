package edu.cornell.cs.apl.viaduct;

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
public class ProgramDependencyGraph {
  HashSet<PdgNode> nodes;

  public ProgramDependencyGraph() {
    this.nodes = new HashSet<PdgNode>();
  }

  public void addNode(PdgNode node) {
    this.nodes.add(node);
  }

  public Set<PdgNode> getNodes() {
    return this.nodes;
  }

  @Override
  public String toString() {
    List<PdgNode> sortedNodes = new ArrayList<PdgNode>(this.nodes);
    Collections.sort(sortedNodes);

    StringBuffer buf = new StringBuffer();
    for (PdgNode node : sortedNodes) {
      buf.append(node.toString() + "\n");
    }

    return buf.toString();
  }
}
