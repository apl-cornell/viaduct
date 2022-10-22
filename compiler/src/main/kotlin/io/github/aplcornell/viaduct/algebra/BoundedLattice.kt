package io.github.apl_cornell.viaduct.algebra

/** Provides the least and greatest elements in a [Lattice]. */
interface BoundedLattice<T : Lattice<T>> : BoundedJoinSemiLattice<T>, BoundedMeetSemiLattice<T>
