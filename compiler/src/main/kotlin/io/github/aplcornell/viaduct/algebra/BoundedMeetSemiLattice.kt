package io.github.aplcornell.viaduct.algebra

/** Provides the identity element in a [MeetSemiLattice]. */
interface BoundedMeetSemiLattice<T : MeetSemiLattice<T>> {
    /** The greatest element of [T]. */
    val top: T
}
