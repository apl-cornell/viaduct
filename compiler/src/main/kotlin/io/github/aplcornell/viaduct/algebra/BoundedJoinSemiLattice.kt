package io.github.aplcornell.viaduct.algebra

/** Provides the identity element in a [JoinSemiLattice]. */
interface BoundedJoinSemiLattice<T : JoinSemiLattice<T>> {
    /** The least element of [T]. */
    val bottom: T
}
