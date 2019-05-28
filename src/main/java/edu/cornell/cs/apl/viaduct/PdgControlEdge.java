package edu.cornell.cs.apl.viaduct;

/* represents control flow relationship between nodes. */
public class PdgControlEdge<T extends AstNode> extends PdgEdge<T> {
  public PdgControlEdge(PdgNode<T> s, PdgNode<T> t, String l) {
    super(s, t);
    this.label = l;
  }

  /** create a control edge b/w nodes. */
  public static PdgControlEdge create(PdgNode source, PdgNode target, String label) {
    PdgControlEdge controlEdge = new PdgControlEdge(source, target, label);
    source.addOutControlEdge(controlEdge);
    target.setInControlEdge(controlEdge);
    return controlEdge;
  }
}
