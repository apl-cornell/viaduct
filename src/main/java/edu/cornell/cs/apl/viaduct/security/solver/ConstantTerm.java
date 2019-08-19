package edu.cornell.cs.apl.viaduct.security.solver;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import edu.cornell.cs.apl.viaduct.util.HeytingAlgebra;

/** Term that represents a constant element. */
@AutoValue
public abstract class ConstantTerm<A extends HeytingAlgebra<A>> extends ConstraintValue<A> {
  private static final String CONST_ID = "const";
  private static final FreshNameGenerator nameGenerator = new FreshNameGenerator();

  public abstract A getValue();

  protected ConstantTerm() {
    super(nameGenerator.getFreshName(CONST_ID));
  }

  public static <V extends HeytingAlgebra<V>> ConstantTerm<V> create(V value) {
    return new AutoValue_ConstantTerm<>(value);
  }

  /** Return a term that represents the meet of {@code this} and {@code that}. */
  public final LeftHandTerm<A> meet(ConstraintValue<A> that) {
    if (that instanceof ConstantTerm) {
      final ConstantTerm<A> other = (ConstantTerm<A>) that;
      return ConstantTerm.create(this.getValue().meet(other.getValue()));
    } else if (that instanceof VariableTerm) {
      final VariableTerm<A> other = (VariableTerm<A>) that;
      return ConstantMeetVariableTerm.create(this.getValue(), other);
    } else {
      throw new IllegalArgumentException("Argument must be a variable or a constant.");
    }
  }

  /** Return a term that represents the join of {@code this} and {@code that}. */
  public final RightHandTerm<A> join(ConstraintValue<A> that) {
    if (that instanceof ConstantTerm) {
      final ConstantTerm<A> other = (ConstantTerm<A>) that;
      return ConstantTerm.create(this.getValue().join(other.getValue()));
    } else if (that instanceof VariableTerm) {
      final VariableTerm<A> other = (VariableTerm<A>) that;
      return ConstantJoinVariableTerm.create(this.getValue(), other);
    } else {
      throw new IllegalArgumentException("Argument must be a variable or a constant.");
    }
  }

  @Override
  public final A initialize() {
    return getValue();
  }

  @Override
  public final A transfer(A newValue) {
    if (!getValue().lessThanOrEqualTo(newValue)) {
      // Constants cannot be updated, so there is no way to satisfy the constraint.
      throw new UnsatisfiableConstraintException(this, getValue(), newValue);
    }

    return getValue();
  }
}
