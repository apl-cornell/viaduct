package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Lattice;

import java.util.HashSet;
import java.util.Set;

/** dataflow analysis for PDGs. parameterized by lattice defined by subclasses. */
public abstract class PdgDataflow<T extends Lattice<T>, U extends AstNode>
    extends Dataflow<T, PdgNode<U>> {

  @Override
  protected Set<PdgNode<U>> getInNodes(PdgNode<U> node) {
    Set<PdgNode<U>> inNodes = new HashSet<>();
    for (PdgEdge<U> inEdge : node.getInInfoEdges()) {
      inNodes.add(inEdge.getSource());
    }

    return inNodes;
  }
}
