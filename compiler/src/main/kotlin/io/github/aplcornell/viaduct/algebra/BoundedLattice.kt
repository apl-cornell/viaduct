package io.github.aplcornell.viaduct.algebra

/** Provides the least and greatest elements in a [Lattice]. */
interface BoundedLattice<T : Lattice<T>> : BoundedJoinSemiLattice<T>, BoundedMeetSemiLattice<T>
