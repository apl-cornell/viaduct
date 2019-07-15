package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.AstPrinter;
import edu.cornell.cs.apl.viaduct.Binding;

/** a read from a computation / subexpression. */
public final class PdgComputeEdge<T extends AstNode> extends PdgReadEdge<T> {
  private final Binding<T> binding;

  private PdgComputeEdge(PdgNode<T> source, PdgNode<T> target, Binding<T> binding) {
    super(source, target);
    this.binding = binding;
  }

  /** create edge b/w nodes. */
  public static <T extends AstNode> PdgComputeEdge<T> create(
      PdgNode<T> source, PdgNode<T> target, Binding<T> binding) {

    PdgComputeEdge<T> readEdge = new PdgComputeEdge<>(source, target, binding);
    source.addOutInfoEdge(readEdge);
    target.addInInfoEdge(readEdge);
    return readEdge;
  }

  public Binding<T> getBinding() {
    return this.binding;
  }

  @Override
  public boolean isQueryEdge() {
    return false;
  }

  @Override
  public boolean isComputeEdge() {
    return true;
  }

  @Override
  public String getLabel(AstPrinter<T> printer) {
    return this.binding.getBinding();
  }
}
