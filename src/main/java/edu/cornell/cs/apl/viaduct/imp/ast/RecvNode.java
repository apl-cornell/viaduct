package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** receive value from a process. */
public class RecvNode extends StmtNode {
  private final Variable var;
  private final String sender;

  public RecvNode(String s, Variable v) {
    this.var = v;
    this.sender = s;
  }

  public String getSender() {
    return this.sender;
  }

  public Variable getVar() {
    return this.var;
  }

  @Override
  public <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
