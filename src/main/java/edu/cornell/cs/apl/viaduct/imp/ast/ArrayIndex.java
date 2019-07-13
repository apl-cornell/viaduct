package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;

/** Reference to an index in an array. */
@AutoValue
public abstract class ArrayIndex implements Reference {
  public static ArrayIndex create(Variable array, ExpressionNode index) {
    return new AutoValue_ArrayIndex(array, index);
  }

  public abstract Variable getArray();

  public abstract ExpressionNode getIndex();

  @Override
  public final <R> R accept(ReferenceVisitor<R> v) {
    return v.visit(this);
  }
}
