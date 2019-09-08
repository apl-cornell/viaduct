package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** While loops. */
@AutoValue
public abstract class WhileNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_WhileNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ExpressionNode getGuard();

  public abstract BlockNode getBody();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setGuard(ExpressionNode guard);

    public abstract Builder setBody(BlockNode body);

    public abstract BlockNode.Builder bodyBuilder();

    public abstract WhileNode build();
  }
}
