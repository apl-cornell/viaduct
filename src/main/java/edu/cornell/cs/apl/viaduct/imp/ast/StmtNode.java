package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.AstVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** generic statement interface for visitors. */
public abstract class StmtNode extends ImpAstNode {
  public abstract <R> R accept(StmtVisitor<R> v);

  public <R> R accept(AstVisitor<R> v) {
    return this.accept((StmtVisitor<R>)v);
  }
}
