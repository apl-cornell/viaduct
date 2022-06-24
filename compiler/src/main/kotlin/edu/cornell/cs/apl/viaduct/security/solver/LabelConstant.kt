package edu.cornell.cs.apl.viaduct.security.solver

import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice
import edu.cornell.cs.apl.viaduct.algebra.solver.ConstantTerm
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.Principal

/** Terms representing literal label constants. */
data class LabelConstant(val value: Label) : AtomicLabelTerm() {
    override fun getValue(solution: ConstraintSolution): Label {
        return value
    }

    override fun confidentiality(): LabelConstant {
        return LabelConstant(value.confidentiality())
    }

    override fun integrity(): LabelConstant {
        return LabelConstant(value.integrity())
    }

    override fun swap(): LabelConstant {
        return LabelConstant(value.swap())
    }

    override fun join(that: Label): LabelConstant {
        return LabelConstant(value.join(that))
    }

    override val confidentialityComponent: ConstantTerm<FreeDistributiveLattice<Principal>>
        get() = ConstantTerm(value.confidentialityComponent)

    override val integrityComponent: ConstantTerm<FreeDistributiveLattice<Principal>>
        get() = ConstantTerm(value.integrityComponent)
}
