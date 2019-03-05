package edu.cornell.cs.apl.viaduct.surface;

import edu.cornell.cs.apl.viaduct.StmtVisitor;
import java.util.List;

/** Sequences a list of statements. */
public class BlockNode implements StmtNode {
  private final List<StmtNode> statements;

  public BlockNode(List<StmtNode> statements) {
    this.statements = statements;
  }

  public List<StmtNode> getStatements() {
    return statements;
  }

  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("(");
    for (StmtNode stmt : this.getStatements()) {
      buf.append(stmt.toString());
    }
    buf.append(")");
    return buf.toString();
  }
}
