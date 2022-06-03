package io.github.apl_cornell.viaduct.security.solver

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.solver.LeftHandTerm
import io.github.apl_cornell.viaduct.algebra.solver.RightHandTerm
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.Principal

/** A symbolic representation of a label expression. */
abstract class LabelTerm {
    /** Returns the value of this term given an assignment of values to every variable in the term. */
    abstract fun getValue(solution: ConstraintSolution): Label

    /** Term that corresponds to performing [Label.confidentiality]. */
    abstract fun confidentiality(): LabelTerm?

    /** Term that corresponds to performing [Label.integrity]. */
    abstract fun integrity(): LabelTerm?

    /** Returns a term representing the confidentiality component. */
    internal abstract val confidentialityComponent: LeftHandTerm<FreeDistributiveLattice<Principal>>

    /** Returns a term representing the integrity component. */
    internal abstract val integrityComponent: RightHandTerm<FreeDistributiveLattice<Principal>>
}
