package io.github.apl_cornell.viaduct.algebra

/** A set with unique least upper bounds and greatest lower bounds. */
interface Lattice<T : Lattice<T>> : JoinSemiLattice<T>, MeetSemiLattice<T>
