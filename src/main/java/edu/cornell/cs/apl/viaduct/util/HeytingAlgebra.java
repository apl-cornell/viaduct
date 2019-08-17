package edu.cornell.cs.apl.viaduct.util;

/**
 * A Heyting algebra is a bounded lattice that supports a "relative pseudocomplement"
 * operation {@code ->} where {@code A -> B} defines a maximal element x
 * such that {@code A & x <= B}.
 * This is also called an implicated lattice.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Pseudocomplement#Relative_pseudocomplement">Wikipedia page on pseudocomplement</a>
 */
public interface HeytingAlgebra<T extends HeytingAlgebra<T>> extends Lattice<T> {

  /**
   * {@code t1.relativePseudocomplement(t2)} is the greatest solution to
   * {@code t2.meet(x).lessThanOrEqualTo(t1)}.
   */
  T relativePseudocomplement(T that);
}
