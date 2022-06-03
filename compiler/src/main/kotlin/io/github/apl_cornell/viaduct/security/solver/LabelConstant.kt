package io.github.apl_cornell.viaduct.security.solver

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.solver.ConstantTerm
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.Principal

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
