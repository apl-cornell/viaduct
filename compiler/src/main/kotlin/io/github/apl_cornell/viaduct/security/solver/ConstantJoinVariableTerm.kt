package io.github.apl_cornell.viaduct.security.solver

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.solver.LeftHandTerm
import io.github.apl_cornell.viaduct.algebra.solver.RightHandTerm
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.Principal

internal data class ConstantJoinVariableTerm(val lhs: Label, val rhs: LabelVariable) : LabelTerm() {
    override fun getValue(solution: ConstraintSolution): Label {
        return lhs.join(rhs.getValue(solution))
    }

    override fun confidentiality(): LabelTerm {
        return ConstantJoinVariableTerm(lhs.confidentiality(), rhs.confidentiality())
    }

    override fun integrity(): LabelTerm {
        return ConstantJoinVariableTerm(lhs.integrity(), rhs.integrity())
    }

    override val confidentialityComponent: LeftHandTerm<FreeDistributiveLattice<Principal>>
        get() = rhs.confidentialityComponent.meet(lhs.confidentialityComponent)

    override val integrityComponent: RightHandTerm<FreeDistributiveLattice<Principal>>
        get() = rhs.integrityComponent.join(lhs.integrityComponent)
}
