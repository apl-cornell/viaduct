package io.github.apl_cornell.viaduct.security.solver

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.solver.AtomicTerm
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.Principal

/** An atomic term such as a constant or a variable but a join. */
sealed class AtomicLabelTerm : LabelTerm() {
    abstract override fun confidentiality(): AtomicLabelTerm

    abstract override fun integrity(): AtomicLabelTerm

    /** The term that corresponds to performing [Label.swap]. */
    abstract fun swap(): AtomicLabelTerm

    /** The term that corresponds to the [Label.join] of `this` and [that]. */
    abstract fun join(that: Label): LabelTerm

    abstract override val confidentialityComponent: AtomicTerm<FreeDistributiveLattice<Principal>>

    abstract override val integrityComponent: AtomicTerm<FreeDistributiveLattice<Principal>>
}
