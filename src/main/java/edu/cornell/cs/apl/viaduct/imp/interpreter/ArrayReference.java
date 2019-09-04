package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.UnavailableValue;
import io.vavr.collection.Array;
import java.util.Arrays;

/** Reference to an array. */
final class ArrayReference extends AllocatedObject {
  private final ImpValue[] array;

  private ArrayReference(int size) {
    this.array = new ImpValue[size];
    Arrays.fill(array, UnavailableValue.create());
  }

  /** Allocate an array of given size and return a reference to it. */
  static ArrayReference allocate(int size) {
    return new ArrayReference(size);
  }

  @Override
  ImpValue get() {
    throw new UnsupportedOperationException("Cannot read from an array reference.");
  }

  @Override
  void set(ImpValue newValue) {
    throw new UnsupportedOperationException("Cannot write to an array reference.");
  }

  @Override
  Reference index(int index) {
    return ArrayIndexReference.create(array, index);
  }

  @Override
  int size() {
    return array.length;
  }

  @Override
  Object getImmutableValue() {
    return Array.of(this.array);
  }
}
