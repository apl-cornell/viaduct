package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import io.vavr.collection.Vector;
import java.util.Iterator;
import java.util.Objects;
import javax.annotation.Nonnull;

/** Sequences a list of statements. */
public class BlockNode extends StmtNode implements Iterable<StmtNode> {
  private final Vector<StmtNode> statements;

  public BlockNode(StmtNode... statements) {
    this.statements = Vector.of(statements);
  }

  public BlockNode(Iterable<? extends StmtNode> statements) {
    this.statements = Vector.ofAll(statements);
  }

  /** flatten nested blocks. */
  /*
  public BlockNode flatten() {
    ArrayList<StmtNode> newList = new ArrayList<>();

    for (StmtNode stmt : statements) {
      if (stmt instanceof BlockNode) {
        BlockNode block = (BlockNode) stmt;
        block.flatten();
        for (StmtNode blockStmt : block) {
          newList.add(blockStmt);
        }
      } else {
        newList.add(stmt);
      }
    }
    return new BlockNode(newList);
  }
  */

  public int size() {
    return this.statements.size();
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
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof BlockNode) {
      BlockNode otherBlock = (BlockNode) other;

      if (otherBlock.statements.length() != this.statements.length()) {
        return false;

      } else {
        boolean allEquals = true;
        for (int i = 0; i < this.statements.length(); i++) {
          allEquals = allEquals && otherBlock.statements.get(i).equals(this.statements.get(i));
        }

        return allEquals;
      }

    } else {
      return false;
    }
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
