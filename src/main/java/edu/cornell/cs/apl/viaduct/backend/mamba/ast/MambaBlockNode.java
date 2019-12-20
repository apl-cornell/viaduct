package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaStatementVisitor;
import java.util.Iterator;
import javax.annotation.Nonnull;

@AutoValue
public abstract class MambaBlockNode implements MambaStatementNode, Iterable<MambaStatementNode> {
  private static final MambaBlockNode EMPTY_BLOCK = MambaBlockNode.builder().build();

  public static MambaBlockNode create(Iterable<MambaStatementNode> stmts) {
    return builder().addAll(stmts).build();
  }

  public static Builder builder() {
    return new AutoValue_MambaBlockNode.Builder();
  }

  public static MambaBlockNode empty() {
    return EMPTY_BLOCK;
  }

  public abstract Builder toBuilder();

  public abstract ImmutableList<MambaStatementNode> getStatements();

  @Override
  public final <R> R accept(MambaStatementVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public final @Nonnull Iterator<MambaStatementNode> iterator() {
    return getStatements().iterator();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setStatements(Iterable<? extends MambaStatementNode> statements);

    public abstract ImmutableList.Builder<MambaStatementNode> statementsBuilder();

    public final Builder add(MambaStatementNode statement) {
      statementsBuilder().add(statement);
      return this;
    }

    public final Builder addAll(Iterable<? extends MambaStatementNode> statements) {
      statementsBuilder().addAll(statements);
      return this;
    }

    public abstract MambaBlockNode build();
  }
}
