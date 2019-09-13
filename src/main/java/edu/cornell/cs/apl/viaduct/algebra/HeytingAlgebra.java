package edu.cornell.cs.apl.viaduct.algebra;

/**
 * A Heyting algebra is a bounded lattice that supports an implication operation {@code →} where
 * {@code A → B} is the greatest element {@code x} that satisfies {@code A ∧ x ≤ B}.
 *
 * <p>This is also called an implicated lattice.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Pseudocomplement#Relative_pseudocomplement">Wikipedia
 *     page on pseudocomplement</a>
 */
public interface HeytingAlgebra<T extends HeytingAlgebra<T>> extends Lattice<T> {
  /** {@code this.imply(that)} is the greatest solution to {@code this.meet(x) ≤ that}. */
  T imply(T that);
}
