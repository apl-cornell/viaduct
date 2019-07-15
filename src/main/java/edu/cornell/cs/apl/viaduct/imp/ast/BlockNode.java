package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import io.vavr.collection.Vector;
import java.util.Iterator;
import java.util.Objects;
import javax.annotation.Nonnull;

/** Sequences a list of statements. */
public final class BlockNode extends StmtNode implements Iterable<StmtNode> {
  private final Vector<StmtNode> statements;

  public BlockNode(StmtNode... statements) {
    this.statements = Vector.of(statements);
  }

  public BlockNode(Iterable<? extends StmtNode> statements) {
    this.statements = Vector.ofAll(statements);
  }

  /** Return the number of statements in the block. */
  public int size() {
    return this.statements.size();
  }

  public StmtNode getFirstStmt() {
    return this.statements.head();
  }

  public StmtNode getLastStmt() {
    return this.statements.last();
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof BlockNode)) {
      return false;
    }

    final BlockNode that = (BlockNode) o;
    return Objects.equals(this.statements, that.statements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.statements.toArray());
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
