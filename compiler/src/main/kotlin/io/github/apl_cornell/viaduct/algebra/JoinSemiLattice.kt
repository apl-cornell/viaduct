package io.github.apl_cornell.viaduct.algebra

/** A set that supports binary least upper bounds. */
interface JoinSemiLattice<T : JoinSemiLattice<T>> {
    /** The least upper bound of `this` and [that]. */
    infix fun join(that: T): T
}