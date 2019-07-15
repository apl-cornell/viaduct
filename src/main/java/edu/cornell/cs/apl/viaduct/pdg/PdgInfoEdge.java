package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.AstPrinter;

public abstract class PdgInfoEdge<T extends AstNode> extends PdgEdge<T> {
  public PdgInfoEdge(PdgNode<T> source, PdgNode<T> target) {
    super(source, target);
  }

  public boolean isReadEdge() {
    return false;
  }

  public boolean isWriteEdge() {
    return false;
  }

  public boolean isReadChannelEdge() {
    return false;
  }

  public boolean isPcFlowEdge() {
    return false;
  }

  public String getLabel(AstPrinter<T> printer) {
    return "";
  }
}
