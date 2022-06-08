package io.github.apl_cornell.viaduct.algebra

/** A set that supports binary greatest lower bounds. */
interface MeetSemiLattice<T : MeetSemiLattice<T>> {
    /** The greatest lower bound of `this` and [that]. */
    infix fun meet(that: T): T
}