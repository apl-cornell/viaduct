package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Iterator;
import javax.annotation.Nonnull;

/** Sequences a list of statements. */
@AutoValue
public abstract class BlockNode extends StatementNode implements Iterable<StatementNode> {
  private static final BlockNode EMPTY_BLOCK = BlockNode.builder().build();

  public static Builder builder() {
    return new AutoValue_BlockNode.Builder().setDefaults();
  }

  /** The empty block node. */
  public static BlockNode empty() {
    return EMPTY_BLOCK;
  }

  public abstract Builder toBuilder();

  public abstract ImmutableList<StatementNode> getStatements();

  /** Return the number of statements in the block. */
  @Deprecated
  public final int size() {
    return getStatements().size();
  }

  @Deprecated
  public StatementNode getFirstStmt() {
    return getStatements().get(0);
  }

  @Deprecated
  public StatementNode getLastStmt() {
    return getStatements().get(getStatements().size() - 1);
  }

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public final @Nonnull Iterator<StatementNode> iterator() {
    return getStatements().iterator();
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setStatements(Iterable<? extends StatementNode> statements);

    public abstract ImmutableList.Builder<StatementNode> statementsBuilder();

    public final Builder add(StatementNode statement) {
      this.statementsBuilder().add(statement);
      return this;
    }

    public final Builder addAll(Iterable<? extends StatementNode> statements) {
      this.statementsBuilder().addAll(statements);
      return this;
    }

    public abstract BlockNode build();
  }
}
