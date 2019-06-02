package edu.cornell.cs.apl.viaduct;

/** represents reading from a variable or computation. */
public class PdgReadEdge<T extends AstNode> extends PdgInfoEdge<T> {
  public PdgReadEdge(PdgNode<T> source, PdgNode<T> target, Binding<T> b) {
    super(source, target);
    this.label = b;
  }

  public PdgReadEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
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

  @Override
  public boolean isReadEdge() {
    return true;
  }
}
