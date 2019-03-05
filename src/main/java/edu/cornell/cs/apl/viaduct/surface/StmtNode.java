package edu.cornell.cs.apl.viaduct.surface;

import edu.cornell.cs.apl.viaduct.StmtVisitor;

/** generic statement interface for visitors. */
public interface StmtNode extends AstNode {
  <R> R accept(StmtVisitor<R> v);
}
