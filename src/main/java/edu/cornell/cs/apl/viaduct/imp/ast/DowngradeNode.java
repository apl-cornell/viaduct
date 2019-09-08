package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import javax.annotation.Nullable;

/** Reduce the confidentiality and/or integrity of the result of an expression. */
@AutoValue
public abstract class DowngradeNode extends ExpressionNode {
  public static Builder builder() {
    return new AutoValue_DowngradeNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ExpressionNode getExpression();

  @Nullable
  public abstract Label getFromLabel();

  public abstract Label getToLabel();

  public abstract DowngradeType getDowngradeType();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  public enum DowngradeType {
    DECLASSIFY,
    ENDORSE,
    BOTH
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setExpression(ExpressionNode expression);

    public abstract Builder setFromLabel(Label fromLabel);

    public abstract Builder setToLabel(Label toLabel);

    public abstract Builder setDowngradeType(DowngradeType downgradeType);

    public abstract DowngradeNode build();
  }
}
