package edu.cornell.cs.apl.viaduct.algebra

/** A set that supports binary least upper bounds. */
interface JoinSemiLattice<T : JoinSemiLattice<T>> : PartialOrder<T> {
    /** The least upper bound of `this` and [that]. */
    fun join(that: T): T
}
