package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** receive value from a process. */
public class RecvNode extends StmtNode {
  String sender;
  Variable var;

  public RecvNode(String s, Variable v) {
    this.sender = s;
    this.var = v;
  }

  public String getSender() {
    return this.sender;
  }

  public Variable getVar() {
    return this.var;
  }

  public <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
