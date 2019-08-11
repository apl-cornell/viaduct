package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** A literal constant. */
@AutoValue
public abstract class LiteralNode extends ExpressionNode {
  public static LiteralNode create(ImpValue value) {
    return new AutoValue_LiteralNode(value);
  }

  public abstract ImpValue getValue();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }
}
