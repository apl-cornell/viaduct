package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** generic statement interface for visitors. */
public interface StmtNode extends ImpAstNode {
  <R> R accept(StmtVisitor<R> v);
}
