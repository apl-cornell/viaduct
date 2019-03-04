package edu.cornell.cs.apl.viaduct;

import java.util.HashSet;
import java.util.Set;

/**
 * represents dependencies (reads/writes) among part of a program. nodes represent declared
 * variables (storage) or computations (compute nodes) edge (A,B) represents a "read" of PDG node A
 * by PDG node B
 */
public class ProgramDependencyGraph {
  Set<PdgNode> nodes;

  public ProgramDependencyGraph() {
    this.nodes = new HashSet<PdgNode>();
  }

  public void add(PdgNode node) {
    this.nodes.add(node);
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (PdgNode node : this.nodes) {
      buf.append(node.toString() + "\n");
    }

    return buf.toString();
  }
}
