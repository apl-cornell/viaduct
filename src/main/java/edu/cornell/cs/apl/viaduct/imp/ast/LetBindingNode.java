package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Declare and assign temporary variables. */
@AutoValue
public abstract class LetBindingNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_LetBindingNode.Builder().setDefaults();
  }

  public abstract Builder toBuilder();

  public abstract Variable getVariable();

  public abstract ExpressionNode getRhs();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setVariable(Variable variable);

    public abstract Builder setRhs(ExpressionNode rhs);

    public abstract LetBindingNode build();
  }
}
