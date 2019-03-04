package edu.cornell.cs.apl.viaduct;

import java.util.List;

/** sequences a list of statements. */
public class SeqNode implements StmtNode {
  List<StmtNode> stmts;

  public SeqNode(List<StmtNode> stmts) {
    this.stmts = stmts;
  }

  public List<StmtNode> getStmts() {
    return this.stmts;
  }

  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("(");
    for (StmtNode stmt : this.stmts) {
      buf.append(stmt.toString());
    }
    buf.append(")");
    return buf.toString();
  }
}
