package edu.cornell.cs.apl.viaduct.security.solver

import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice
import edu.cornell.cs.apl.viaduct.algebra.solver.LeftHandTerm
import edu.cornell.cs.apl.viaduct.algebra.solver.RightHandTerm
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.Principal

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
