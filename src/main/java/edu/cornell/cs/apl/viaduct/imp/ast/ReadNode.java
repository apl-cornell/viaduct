package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Read the value pointed to by a reference. */
@AutoValue
public abstract class ReadNode extends ExpressionNode {
  public static ReadNode create(Reference reference) {
    return new AutoValue_ReadNode(reference);
  }

  public abstract Reference getReference();

  @Override
  public final <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }
}
