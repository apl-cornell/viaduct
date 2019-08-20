package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;

import javax.annotation.Nullable;

/** Reduce the confidentiality and/or integrity of the result of an expression. */
@AutoValue
public abstract class DowngradeNode extends ExpressionNode {
  public enum DowngradeType { DECLASSIFY, ENDORSE, BOTH }

  public static DowngradeNode create(ExpressionNode expression, Label toLabel) {
    return new AutoValue_DowngradeNode(expression, null, toLabel, DowngradeType.BOTH);
  }

  public static DowngradeNode create(
      ExpressionNode expression, @Nullable Label fromLabel, Label toLabel) {
    return new AutoValue_DowngradeNode(expression, fromLabel, toLabel, DowngradeType.BOTH);
  }

  public static DowngradeNode create(
      ExpressionNode expression, Label toLabel, DowngradeType downgradeType) {
    return new AutoValue_DowngradeNode(expression, null, toLabel, downgradeType);
  }

  public static DowngradeNode create(
      ExpressionNode expression, @Nullable Label fromLabel, Label toLabel,
      DowngradeType downgradeType) {
    return new AutoValue_DowngradeNode(expression, fromLabel, toLabel, downgradeType);
  }

  public abstract ExpressionNode getExpression();

  @Nullable
  public abstract Label getFromLabel();

  public abstract Label getLabel();

  public abstract DowngradeType getDowngradeType();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }
}
