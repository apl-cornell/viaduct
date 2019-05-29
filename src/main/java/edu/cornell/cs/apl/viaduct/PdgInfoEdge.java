package edu.cornell.cs.apl.viaduct;

public abstract class PdgInfoEdge<T extends AstNode> extends PdgEdge<T> {
  String label;

  public PdgInfoEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  public String getLabel() {
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
