package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import javax.annotation.Nonnull;

/** A mutable variable that names a memory location. */
@AutoValue
public abstract class Variable extends ReferenceNode
    implements Binding<ImpAstNode>, Comparable<Variable>, Name {
  public static Variable create(String name) {
    return builder().setName(name).build();
  }

  public static <T extends AstNode> Variable create(Binding<T> binding) {
    return builder().setName(binding.getBinding()).build();
  }

  public static Builder builder() {
    return new AutoValue_Variable.Builder();
  }

  public abstract Builder toBuilder();

  @Override
  public final String getNameCategory() {
    return "variable";
  }

  @Override
  public final String getBinding() {
    return getName();
  }

  @Override
  public final <R> R accept(ReferenceVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public int compareTo(@Nonnull Variable that) {
    return this.getName().compareTo(that.getName());
  }

  @Override
  public final String toString() {
    return getName();
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setName(String name);

    public abstract Variable build();
  }
}
