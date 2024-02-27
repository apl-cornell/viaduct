package io.github.aplcornell.viaduct.security

import io.github.aplcornell.viaduct.algebra.BoundedLattice
import io.github.aplcornell.viaduct.algebra.Lattice
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable

/**
 * A lattice for information flow security. This is a product lattice with
 * confidentiality and integrity components. Information flows from less
 * restrictive contexts to more restrictive ones.
 *
 * Elements of [T] are interpreted as principals.
 *
 * [meet] and [join] talk about information flow.
 *
 * [and] and [or] talk about trust.
 */
class SecurityLattice<T : Lattice<T>>(
    /**
     * The confidentiality component in the underlying lattice.
     *
     * Unlike [confidentiality], the result is not a [SecurityLattice].
     */
    val confidentialityComponent: T,
    /**
     * The integrity component in the underlying lattice.
     *
     * Unlike [integrity], the result is not a [SecurityLattice].
     */
    val integrityComponent: T,
) : Lattice<SecurityLattice<T>>, TrustLattice<SecurityLattice<T>>, PrettyPrintable {
    /** Returns an element with [confidentialityComponent] and [integrityComponent] equal to [principal]. */
    constructor(principal: T) : this(principal, principal)

    /**
     * Returns an element that represents the confidentiality component.
     *
     * Keeps confidentiality the same while setting integrity to the weakest level.
     */
    fun confidentiality(bounds: BoundedLattice<T>): SecurityLattice<T> = SecurityLattice(confidentialityComponent, weakest(bounds))

    /**
     * Returns an element that represents the integrity component.
     *
     * Keeps integrity the same while setting confidentiality to the weakest level.
     */
    fun integrity(bounds: BoundedLattice<T>): SecurityLattice<T> = SecurityLattice(weakest(bounds), integrityComponent)

    override infix fun join(that: SecurityLattice<T>): SecurityLattice<T> {
        val confidentiality = this.confidentialityComponent meet that.confidentialityComponent
        val integrity = this.integrityComponent join that.integrityComponent
        return SecurityLattice(confidentiality, integrity)
    }

    override infix fun meet(that: SecurityLattice<T>): SecurityLattice<T> {
        val confidentiality = this.confidentialityComponent join that.confidentialityComponent
        val integrity = this.integrityComponent meet that.integrityComponent
        return SecurityLattice(confidentiality, integrity)
    }

    override infix fun or(that: SecurityLattice<T>): SecurityLattice<T> {
        val confidentiality = this.confidentialityComponent join that.confidentialityComponent
        val integrity = this.integrityComponent join that.integrityComponent
        return SecurityLattice(confidentiality, integrity)
    }

    override infix fun and(that: SecurityLattice<T>): SecurityLattice<T> {
        val confidentiality = this.confidentialityComponent meet that.confidentialityComponent
        val integrity = this.integrityComponent meet that.integrityComponent
        return SecurityLattice(confidentiality, integrity)
    }

    /**
     * Switches the confidentiality and integrity components.
     *
     * This is used to enforce robust declassification and transparent endorsement,
     * a.k.a. [nonmalleable information flow control](https://dl.acm.org/doi/10.1145/3133956.3134054).
     * TODO: Swap components as well?
     */
    fun swap(): SecurityLattice<T> = SecurityLattice(integrityComponent, confidentialityComponent)

    // TODO: we can do better
    override fun toString(): String {
        val confidentialityStr = confidentialityComponent.toString()
        val integrityStr = integrityComponent.toString()
        val expression =
            if (confidentialityComponent == integrityComponent) {
                confidentialityStr
            } else {
                "$confidentialityStr-> ∧ $integrityStr<-"
            }
        return "{$expression}"
    }

    override fun toDocument() = Document(this.toString())

    /** Provides bounds for a [SecurityLattice] given bounds for [T]. */
    class Bounds<T : Lattice<T>>(
        bounds: BoundedLattice<T>,
    ) : BoundedLattice<SecurityLattice<T>>, BoundedTrustLattice<SecurityLattice<T>> {
        override val strongest: SecurityLattice<T> =
            SecurityLattice(strongest(bounds), strongest(bounds))

        override val weakest: SecurityLattice<T> =
            SecurityLattice(weakest(bounds), weakest(bounds))

        override val bottom: SecurityLattice<T> =
            SecurityLattice(weakest(bounds), strongest(bounds))

        override val top: SecurityLattice<T> =
            SecurityLattice(strongest(bounds), weakest(bounds))
    }

    private companion object {
        /** Returns the strongest principal in [T]. */
        private fun <T : Lattice<T>> strongest(bounds: BoundedLattice<T>): T = bounds.bottom

        /** Returns the weakest principal in [T]. */
        private fun <T : Lattice<T>> weakest(bounds: BoundedLattice<T>): T = bounds.top
    }
}
