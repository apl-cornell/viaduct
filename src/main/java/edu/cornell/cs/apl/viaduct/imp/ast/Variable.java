package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;

/** A mutable variable that names a memory location. */
@AutoValue
public abstract class Variable implements Reference, Binding<ImpAstNode> {
  public static Variable create(String name) {
    return new AutoValue_Variable(name);
  }

  public static <T extends AstNode> Variable create(Binding<T> binding) {
    return create(binding.getBinding());
  }

  public abstract String getName();

  @Override
  public final String getBinding() {
    return getName();
  }

  @Override
  public final <R> R accept(ReferenceVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public final String toString() {
    return getName();
  }
}
