package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** generic statement interface for visitors. */
public abstract class StmtNode implements ImpAstNode {
  protected String id;

  protected StmtNode() {
    this.id = null;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public abstract <R> R accept(StmtVisitor<R> v);
}
