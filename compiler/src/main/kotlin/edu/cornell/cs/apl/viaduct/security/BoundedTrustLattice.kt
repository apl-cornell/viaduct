package edu.cornell.cs.apl.viaduct.security

/** Provides the weakest and strongest principals in a [TrustLattice]. */
interface BoundedTrustLattice<T : TrustLattice<T>> {
    /**
     * The most powerful principal.
     *
     * This is the identity for [or].
     */
    val strongest: T

    /**
     * The least powerful principal.
     *
     * This is the identity for [and].
     */
    val weakest: T
}
