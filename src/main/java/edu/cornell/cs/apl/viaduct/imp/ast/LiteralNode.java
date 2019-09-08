package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** A literal constant. */
@AutoValue
public abstract class LiteralNode extends ExpressionNode {
  public static Builder builder() {
    return new AutoValue_LiteralNode.Builder().setDefaults();
  }

  public abstract Builder toBuilder();

  public abstract ImpValue getValue();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setValue(ImpValue value);

    public abstract LiteralNode build();
  }
}
