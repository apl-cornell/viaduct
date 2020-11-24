package edu.cornell.cs.apl.viaduct.algebra

/**
 * A Heyting algebra is a bounded lattice that supports an implication operation `→` where
 * `A → B` is the greatest element `x` that satisfies `A ∧ x ≤ B`.
 *
 * This is also called an implicated lattice.
 *
 * @see [Wikipedia page on pseudocomplement](https://en.wikipedia.org/wiki/Pseudocomplement.Relative_pseudocomplement)
 */
interface HeytingAlgebra<T : HeytingAlgebra<T>> : Lattice<T> {
    /** `this.imply(that)` is the greatest solution to `this.meet(x) ≤ that`. */
    fun imply(that: T): T
}
