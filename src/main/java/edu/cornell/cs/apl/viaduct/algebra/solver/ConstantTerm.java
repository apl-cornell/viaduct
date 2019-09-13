package edu.cornell.cs.apl.viaduct.algebra.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import java.util.Map;
import java.util.Optional;

/** Term representing a constant element. */
@AutoValue
public abstract class ConstantTerm<A extends HeytingAlgebra<A>> extends AtomicTerm<A> {
  /** Create a term that represents the given value. */
  public static <V extends HeytingAlgebra<V>> ConstantTerm<V> create(V value) {
    return new AutoValue_ConstantTerm<>(value);
  }

  public abstract A getValue();

  @Override
  public A getValue(Map<VariableTerm<A>, A> solution) {
    return getValue();
  }

  @Override
  public final ConstantTerm<A> meet(A that) {
    return create(this.getValue().meet(that));
  }

  @Override
  public final ConstantTerm<A> join(A that) {
    return create(this.getValue().join(that));
  }

  @Override
  public final A initialize() {
    return getValue();
  }

  @Override
  public final Optional<A> transfer(A in) {
    if (!getValue().lessThanOrEqualTo(in)) {
      // Constants cannot be updated, so there is no way to satisfy the constraint.
      return Optional.empty();
    }
    return Optional.of(getValue());
  }

  @Override
  public final String toString() {
    return getValue().toString();
  }
}
