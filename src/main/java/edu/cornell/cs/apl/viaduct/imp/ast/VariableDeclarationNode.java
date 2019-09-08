package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import javax.annotation.Nullable;

/** Variable declaration. */
@AutoValue
public abstract class VariableDeclarationNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_VariableDeclarationNode.Builder().setDefaults();
  }

  public abstract Builder toBuilder();

  public abstract Variable getVariable();

  public abstract ImpBaseType getType();

  @Nullable
  public abstract Label getLabel();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setVariable(Variable variable);

    public abstract Builder setType(ImpBaseType type);

    public abstract Builder setLabel(Label label);

    public abstract VariableDeclarationNode build();
  }
}
