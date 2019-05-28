package edu.cornell.cs.apl.viaduct;

public class PdgReadEdge<T extends AstNode> extends PdgInfoEdge<T> {
  Binding<T> binding;

  public PdgReadEdge(PdgNode<T> source, PdgNode<T> target, Binding<T> b) {
    super(source, target);
    this.binding = b;
  }

  public PdgReadEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  public Binding<T> getBinding() {
    return this.binding;
  }

  @Override
  public String getLabel() {
    if (this.binding != null) {
      return this.binding.toString();
    } else {
      return null;
    }
  }

  /** create edge b/w nodes. */
  public static PdgReadEdge create(PdgNode source, PdgNode target) {
    PdgReadEdge readEdge = new PdgReadEdge(source, target);
    source.addOutInfoEdge(readEdge);
    target.addInInfoEdge(readEdge);
    return readEdge;
  }

  /** create edge b/w nodes. */
  public static PdgReadEdge create(PdgNode source, PdgNode target, Binding binding) {
    PdgReadEdge readEdge = new PdgReadEdge(source, target, binding);
    source.addOutInfoEdge(readEdge);
    target.addInInfoEdge(readEdge);
    return readEdge;
  }
}
