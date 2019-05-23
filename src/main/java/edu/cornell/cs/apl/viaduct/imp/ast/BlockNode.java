package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import io.vavr.collection.Vector;
import java.util.Iterator;
import javax.annotation.Nonnull;

/** Sequences a list of statements. */
public class BlockNode extends StmtNode implements Iterable<StmtNode> {
  private final Vector<StmtNode> statements;

  public BlockNode(Iterable<? extends StmtNode> statements) {
    this.statements = Vector.ofAll(statements);
  }

  @Override
  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public @Nonnull Iterator<StmtNode> iterator() {
    return this.statements.iterator();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("(");
    for (StmtNode stmt : this) {
      buf.append(stmt.toString());
    }
    buf.append(")");
    return buf.toString();
  }
}
