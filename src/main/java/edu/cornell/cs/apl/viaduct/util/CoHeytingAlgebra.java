package edu.cornell.cs.apl.viaduct.util;

/**
 * A co-Heyting algebra is a bounded lattice that support a "subtraction" operation, which is an
 * inverse to {@link #join}.
 */
public interface CoHeytingAlgebra<T extends CoHeytingAlgebra<T>> extends Lattice<T> {

  /**
   * Inverse to {@link #join}. {@code t1.subtract(t2)} is the least solution to {@code
   * t1.lessThanOrEqualTo(t2.join(x))}.
   */
  T subtract(T that);
}
