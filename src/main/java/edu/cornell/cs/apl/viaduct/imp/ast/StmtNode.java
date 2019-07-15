package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Generic statement interface for visitors. */
public abstract class StmtNode implements ImpAstNode {
  private String id;

  protected StmtNode() {
    this.id = null;
  }

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public abstract <R> R accept(StmtVisitor<R> v);
}
