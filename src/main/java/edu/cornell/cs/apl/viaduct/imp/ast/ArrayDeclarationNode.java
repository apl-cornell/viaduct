package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import javax.annotation.Nullable;

/** A statically allocated array with a dynamically determined length. */
@AutoValue
public abstract class ArrayDeclarationNode extends StatementNode {
  public static Builder builder() {
    return new AutoValue_ArrayDeclarationNode.Builder().setDefaults();
  }

  public abstract Builder toBuilder();

  /** Name of the array. */
  public abstract Variable getVariable();

  /** Number of elements in the array. */
  public abstract ExpressionNode getLength();

  /** Type of elements in the array. */
  public abstract ImpBaseType getElementType();

  /** Security label of the array and all its elements. Will be inferred if {@code null}. */
  @Nullable
  public abstract Label getLabel();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setVariable(Variable variable);

    public abstract Builder setLength(ExpressionNode length);

    public abstract Builder setElementType(ImpBaseType elementType);

    public abstract Builder setLabel(Label label);

    public abstract ArrayDeclarationNode build();
  }
}
