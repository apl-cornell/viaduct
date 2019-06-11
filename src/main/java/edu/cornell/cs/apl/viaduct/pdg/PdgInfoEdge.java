package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;

public abstract class PdgInfoEdge<T extends AstNode> extends PdgEdge<T> {
  Binding<T> label;

  public PdgInfoEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  public Binding<T> getLabel() {
    return this.label;
  }

  public boolean isReadEdge() {
    return false;
  }

  public boolean isWriteEdge() {
    return false;
  }

  public boolean isFlowEdge() {
    return false;
  }
}
