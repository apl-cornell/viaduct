package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;

/** Reference to a specific index in an array. */
final class ArrayIndexReference extends Reference {
  private final ImpValue[] array;
  private final int index;

  private ArrayIndexReference(ImpValue[] array, int index) {
    this.array = array;
    this.index = index;
  }

  /**
   * Create a reference to a specific index in an array.
   *
   * @param array array being referenced
   * @param index index in that array that is being referenced
   * @throws ArrayIndexOutOfBoundsException if the index lies outside the bounds of the array
   */
  static ArrayIndexReference create(ImpValue[] array, int index) {
    if (0 <= index && index < array.length) {
      return new ArrayIndexReference(array, index);
    } else {
      throw new ArrayIndexOutOfBoundsException(index);
    }
  }

  @Override
  ImpValue get() {
    return array[index];
  }

  @Override
  void set(ImpValue newValue) {
    array[index] = newValue;
  }

  @Override
  Reference index(int i) {
    throw new UnsupportedOperationException("Cannot index into a value reference.");
  }

  @Override
  int size() {
    return 1;
  }
}
