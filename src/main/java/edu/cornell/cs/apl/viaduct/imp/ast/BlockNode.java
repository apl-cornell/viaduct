package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import io.vavr.collection.Vector;
import java.util.Iterator;
import javax.annotation.Nonnull;

/** Sequences a list of statements. */
@AutoValue
public abstract class BlockNode extends StatementNode implements Iterable<StatementNode> {
  public static BlockNode create(StatementNode... statements) {
    return new AutoValue_BlockNode(Vector.of(statements));
  }

  public static BlockNode create(Iterable<? extends StatementNode> statements) {
    return new AutoValue_BlockNode(Vector.ofAll(statements));
  }

  abstract Vector<StatementNode> getStatements();

  /** Return the number of statements in the block. */
  public final int size() {
    return getStatements().size();
  }

  public StatementNode getFirstStmt() {
    return getStatements().head();
  }

  public StatementNode getLastStmt() {
    return getStatements().last();
  }

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public final @Nonnull Iterator<StatementNode> iterator() {
    return getStatements().iterator();
  }
}
