package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;

/** Reference to an index in an array. */
@AutoValue
public abstract class ArrayIndexingNode extends ReferenceNode {
  public static Builder builder() {
    return new AutoValue_ArrayIndexingNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract Variable getArray();

  public abstract ExpressionNode getIndex();

  @Override
  public final <R> R accept(ReferenceVisitor<R> v) {
    return v.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setArray(Variable array);

    public abstract Builder setIndex(ExpressionNode index);

    public abstract ArrayIndexingNode build();
  }
}
