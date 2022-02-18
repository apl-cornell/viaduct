package edu.cornell.cs.apl.viaduct.security.solver

import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice
import edu.cornell.cs.apl.viaduct.algebra.solver.AtomicTerm
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.Principal

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
