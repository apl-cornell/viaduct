package io.github.apl_cornell.viaduct.algebra

/** Provides the identity element in a [JoinSemiLattice]. */
interface BoundedJoinSemiLattice<T : JoinSemiLattice<T>> {
    /** The least element of [T]. */
    val bottom: T
}
