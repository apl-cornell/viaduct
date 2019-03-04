package edu.cornell.cs.apl.viaduct;

/** does nothing. */
public class SkipNode implements StmtNode {
  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(skip)";
  }
}
