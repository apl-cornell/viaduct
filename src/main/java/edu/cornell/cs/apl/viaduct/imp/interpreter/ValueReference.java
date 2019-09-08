package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.UnavailableValue;

/** Reference to a mutable cell that contains a simple (non-array) value. */
final class ValueReference extends AllocatedObject {
  private ImpValue value;

  private ValueReference(ImpValue value) {
    this.value = value;
  }

  /** Allocate a mutable cell for a single value and return a reference to it. */
  static ValueReference allocate() {
    return allocate(UnavailableValue.create());
  }

  /**
   * Allocate a mutable cell with the given initial value.
   *
   * @param initialValue initial value stored in the cell
   */
  static ValueReference allocate(ImpValue initialValue) {
    return new ValueReference(initialValue);
  }

  @Override
  ImpValue get() {
    return this.value;
  }

  @Override
  void set(ImpValue newValue) {
    this.value = newValue;
  }

  @Override
  Reference index(int i) {
    throw new UnsupportedOperationException("Cannot index into a value reference.");
  }

  @Override
  int size() {
    return 1;
  }

  @Override
  Object getImmutableValue() {
    return this.value;
  }
}
