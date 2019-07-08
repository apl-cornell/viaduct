package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;

/** represents reading from a variable or computation. */
public class PdgReadEdge<T extends AstNode> extends PdgInfoEdge<T> {
  private PdgReadEdge(PdgNode<T> source, PdgNode<T> target, Binding<T> b) {
    super(source, target);
    this.label = b;
  }

  private PdgReadEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  /** create edge b/w nodes. */
  public static <T extends AstNode> PdgReadEdge<T> create(PdgNode<T> source, PdgNode<T> target) {
    PdgReadEdge<T> readEdge = new PdgReadEdge<>(source, target);
    source.addOutInfoEdge(readEdge);
    target.addInInfoEdge(readEdge);
    return readEdge;
  }

  /** create edge b/w nodes. */
  public static <T extends AstNode> PdgReadEdge<T> create(
      PdgNode<T> source, PdgNode<T> target, Binding<T> binding) {

    PdgReadEdge<T> readEdge = new PdgReadEdge<>(source, target, binding);
    source.addOutInfoEdge(readEdge);
    target.addInInfoEdge(readEdge);
    return readEdge;
  }

  @Override
  public boolean isReadEdge() {
    return true;
  }
}
