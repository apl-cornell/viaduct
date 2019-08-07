package edu.cornell.cs.apl.viaduct.util;

/**
 * Like {@link Comparable}, but not all pairs of elements have to be ordered.
 *
 * @param <T> the type of objects that this object may be compared to
 */
public interface PartialOrder<T> {
  /**
   * Returns true if {@code this} is ordered before {@code that}. It is not necessary the case that
   * either {@code this.lessThanOrEqualTo(that)} or {@code that.lessThanOrEqualTo(this)}.
   */
  boolean lessThanOrEqualTo(T that);
}
