package edu.cornell.cs.apl.viaduct;

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
