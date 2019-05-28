package edu.cornell.cs.apl.viaduct;

/* represents control flow relationship between nodes. */
public class PdgControlEdge<T extends AstNode> extends PdgEdge<T> {
  static final String DEFAULT_LABEL = "SEQ";

  String label;

  public PdgControlEdge(PdgNode<T> s, PdgNode<T> t, String l) {
    super(s, t);
    this.label = l;
  }

  public PdgControlEdge(PdgNode<T> s, PdgNode<T> t) {
    super(s, t);
    this.label = DEFAULT_LABEL;
  }

  public String getLabel() {
    return this.label;
  }

  public boolean hasDefaultLabel() {
    return this.label.equals(DEFAULT_LABEL);
  }

  /** create a control edge b/w nodes. */
  public static PdgControlEdge create(PdgNode source, PdgNode target) {
    PdgControlEdge controlEdge = new PdgControlEdge(source, target);
    source.addOutControlEdge(controlEdge);
    target.setInControlEdge(controlEdge);
    return controlEdge;
  }

  /** create a control edge b/w nodes. */
  public static PdgControlEdge create(PdgNode source, PdgNode target, String label) {
    PdgControlEdge controlEdge = new PdgControlEdge(source, target, label);
    source.addOutControlEdge(controlEdge);
    target.setInControlEdge(controlEdge);
    return controlEdge;
  }
}
