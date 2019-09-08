package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;

/** A reference to a mutable cell (containing a simple value) or an array. */
abstract class Reference {
  /**
   * Returns the value stored at the referenced location.
   *
   * @throws UnsupportedOperationException if this is an array reference
   */
  abstract ImpValue get();

  /**
   * Sets the value stored at the referenced location.
   *
   * @throws UnsupportedOperationException if this is an array reference
   */
  abstract void set(ImpValue newValue);

  /**
   * Returns a reference to a specific index in the referenced array assuming this is an array
   * reference.
   *
   * @throws UnsupportedOperationException if this is <em>not</em> an array reference
   * @throws ArrayIndexOutOfBoundsException if the index lies outside the array
   */
  abstract Reference index(int i);

  /** Returns the number of elements stored at the referenced location. */
  abstract int size();
}
