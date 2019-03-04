package edu.cornell.cs.apl.viaduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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

  public void add(PdgNode node) {
    this.nodes.add(node);
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
