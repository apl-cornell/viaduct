package edu.cornell.cs.apl.viaduct.security

/** A lattice whose elements are interpreted as principals. */
interface TrustLattice<T : TrustLattice<T>?> {
    /** Decide if `this` is trusted to enforce [that]'s security policies. */
    fun actsFor(that: T): Boolean

    /**
     * The least powerful principal that can act for both `this` and [that].
     * This denotes a conjunction of authority.
     */
    infix fun and(that: T): T

    /**
     * The most powerful principal both `this` and [that] can act for.
     * This denotes a disjunction of authority.
     */
    infix fun or(that: T): T
}
