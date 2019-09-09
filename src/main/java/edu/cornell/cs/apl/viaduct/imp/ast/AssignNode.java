package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Update the value associated with a reference. */
@AutoValue
public abstract class AssignNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_AssignNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ReferenceNode getLhs();

  public abstract ExpressionNode getRhs();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setLhs(ReferenceNode lhs);

    public abstract Builder setRhs(ExpressionNode rhs);

    public abstract AssignNode build();
  }
}
