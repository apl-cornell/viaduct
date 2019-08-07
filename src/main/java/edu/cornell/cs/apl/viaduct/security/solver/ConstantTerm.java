package edu.cornell.cs.apl.viaduct.security.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.util.PartialOrder;

/** Term that represents a constant element. */
@AutoValue
public abstract class ConstantTerm<A extends PartialOrder<A>> implements ConstraintValue<A> {
  public ConstantTerm<A> create(A value) {
    return new AutoValue_ConstantTerm<>(value);
  }

  public abstract A getValue();

  @Override
  public A initialize() {
    return getValue();
  }

  @Override
  public A transfer(A newValue) {
    if (!newValue.lessThanOrEqualTo(getValue())) {
      // Constants cannot be updated, so there is no way to satisfy the constraint.
      throw new UnsatisfiableConstraintException(this);
    }

    return getValue();
  }
}
