package io.github.apl_cornell.viaduct.algebra

/** Provides the identity element in a [MeetSemiLattice]. */
interface BoundedMeetSemiLattice<T : MeetSemiLattice<T>> {
    /** The greatest element of [T]. */
    val top: T
}
