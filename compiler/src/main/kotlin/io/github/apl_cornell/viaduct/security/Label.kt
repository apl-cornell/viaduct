package io.github.apl_cornell.viaduct.security

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.apl.prettyprinting.Style
import io.github.apl_cornell.viaduct.algebra.BoundedLattice
import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.Lattice
import io.github.apl_cornell.viaduct.algebra.PartialOrder
import io.github.apl_cornell.viaduct.security.Label.Companion.bottom
import io.github.apl_cornell.viaduct.security.Label.Companion.strongest
import io.github.apl_cornell.viaduct.security.Label.Companion.top
import io.github.apl_cornell.viaduct.security.Label.Companion.weakest

/**
 * A lattice for information flow security. This is a standard bounded lattice that additionally
 * supports confidentiality and integrity projections. Information flows from less restrictive
 * contexts to more restrictive ones.
 *
 * [top], [bottom], [meet], and [join] talk about information flow.
 *
 * [weakest], [strongest], [and], and [or] talk about trust.
 */
data class Label(
    /**
     * The confidentiality component in the underlying lattice.
     *
     * Unlike [confidentiality], the result is not a [Label].
     */
    val confidentialityComponent: FreeDistributiveLattice<Principal>,

    /**
     * The integrity component in the underlying lattice.
     *
     * Unlike [integrity], the result is not a [Label].
     */
    val integrityComponent: FreeDistributiveLattice<Principal>
) : PartialOrder<Label>, Lattice<Label>, TrustLattice<Label>, PrettyPrintable {
    /**
     * The confidentiality component.
     *
     * Keeps confidentiality the same while setting integrity to the weakest level.
     */
    fun confidentiality(): Label =
        fromConfidentiality(confidentialityComponent)

    /**
     * The integrity component.
     *
     * Keeps integrity the same while setting confidentiality to the weakest level.
     */
    fun integrity(): Label = fromIntegrity(integrityComponent)

    /** Check if information flow from `this` to [that] is safe. */
    infix fun flowsTo(that: Label): Boolean =
        (that.confidentiality() and this.integrity()) actsFor (this.confidentiality() and that.integrity())

    override fun lessThanOrEqualTo(that: Label): Boolean =
        this.flowsTo(that)

    override infix fun join(that: Label): Label =
        (this.confidentiality() and that.confidentiality()) and (this.integrity() or that.integrity())

    override infix fun meet(that: Label): Label =
        (this.confidentiality() or that.confidentiality()) and (this.integrity() and that.integrity())

    override infix fun actsFor(that: Label): Boolean =
        this.confidentialityComponent.lessThanOrEqualTo(that.confidentialityComponent) &&
            this.integrityComponent.lessThanOrEqualTo(that.integrityComponent)

    override infix fun and(that: Label): Label {
        val confidentiality = this.confidentialityComponent.meet(that.confidentialityComponent)
        val integrity = this.integrityComponent.meet(that.integrityComponent)
        return Label(confidentiality, integrity)
    }

    override infix fun or(that: Label): Label {
        val confidentiality = this.confidentialityComponent.join(that.confidentialityComponent)
        val integrity = this.integrityComponent.join(that.integrityComponent)
        return Label(confidentiality, integrity)
    }

    /**
     * Switch the confidentiality and integrity components.
     *
     * This is used to enforce robust declassification and transparent endorsement
     * (a.k.a. non-malleable information flow).
     */
    fun swap(): Label =
        Label(integrityComponent, confidentialityComponent)

    override fun toString(): String {
        val confidentialityStr = confidentialityComponent.toString()
        val integrityStr = integrityComponent.toString()
        val expression =
            when {
                confidentialityComponent == integrityComponent ->
                    confidentialityStr
                this == this.confidentiality() ->
                    "$confidentialityStr->"
                this == this.integrity() ->
                    "$integrityStr<-"
                else ->
                    "$confidentialityStr-> ∧ $integrityStr<-"
            }
        return "{$expression}"
    }

    // TODO: make toDocument primitive and remove toString
    override fun toDocument(): Document = Document(this.toString())

    companion object : BoundedLattice<Label>, BoundedTrustLattice<Label> {
        /**
         * The least powerful principal, i.e. public and untrusted.
         *
         * This is the unit for [and].
         */
        @JvmStatic
        override val weakest: Label = FreeDistributiveLattice.bounds<Principal>().top.let { Label(it, it) }

        /**
         * The most powerful principal, i.e. secret and trusted.
         *
         * This is the unit for [or].
         */
        @JvmStatic
        override val strongest: Label = FreeDistributiveLattice.bounds<Principal>().bottom.let { Label(it, it) }

        /**
         * The least restrictive data policy, i.e. public and trusted.
         *
         * This is the unit for [join].
         */
        @JvmStatic
        override val bottom: Label = Label(weakest.confidentialityComponent, strongest.integrityComponent)

        /**
         * The most restrictive data policy, i.e. secret and untrusted.
         *
         * This is the unit for [meet].
         */
        @JvmStatic
        override val top: Label = Label(strongest.confidentialityComponent, weakest.integrityComponent)

        /** Returns the label representing the authority of the given principal. */
        @JvmStatic
        operator fun invoke(principal: Principal): Label {
            val component = FreeDistributiveLattice(principal)
            return Label(component, component)
        }

        /** Constructs a label given only the confidentiality component. Integrity is set to minimum. */
        @JvmStatic
        fun fromConfidentiality(confidentiality: FreeDistributiveLattice<Principal>): Label =
            Label(confidentiality, weakest.integrityComponent)

        /** Constructs a label given only the integrity component. Confidentiality is set to minimum. */
        @JvmStatic
        fun fromIntegrity(integrity: FreeDistributiveLattice<Principal>): Label =
            Label(weakest.confidentialityComponent, integrity)
    }
}

/** The display style of [Label] specific operators such as [Label.confidentiality]. */
object LabelOperatorStyle : Style
