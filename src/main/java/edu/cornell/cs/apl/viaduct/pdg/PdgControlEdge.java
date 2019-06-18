package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph.ControlLabel;

/* represents control flow relationship between nodes. */
public class PdgControlEdge<T extends AstNode> extends PdgEdge<T> {
  private ControlLabel label;

  private PdgControlEdge(PdgNode<T> s, PdgNode<T> t, ControlLabel l) {
    super(s, t);
    this.label = l;
  }

  /** create a control edge b/w nodes. */
  public static <T extends AstNode> PdgControlEdge create(
      PdgNode<T> source, PdgNode<T> target, ControlLabel label) {
    PdgControlEdge<T> controlEdge = new PdgControlEdge<>(source, target, label);
    source.addOutControlEdge(controlEdge);
    target.setInControlEdge(controlEdge);
    return controlEdge;
  }

  public ControlLabel getLabel() {
    return this.label;
  }
}
