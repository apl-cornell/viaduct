package edu.cornell.cs.apl.viaduct.algebra

/** A set with unique least upper and greatest lower bounds. */
interface Lattice<T : Lattice<T>> : MeetSemiLattice<T>, JoinSemiLattice<T>
