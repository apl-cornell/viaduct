package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

// TODO: remove since BlockNode([]) is the same thing.

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
