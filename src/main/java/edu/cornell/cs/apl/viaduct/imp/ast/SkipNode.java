package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

// TODO: remove since BlockNode([]) is the same thing.

/** does nothing. */
public class SkipNode extends StmtNode {
  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    return other instanceof SkipNode;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return "(skip)";
  }
}
