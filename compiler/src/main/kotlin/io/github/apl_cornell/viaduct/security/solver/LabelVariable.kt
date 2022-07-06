package io.github.apl_cornell.viaduct.security.solver

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.solver.AtomicTerm
import io.github.apl_cornell.viaduct.algebra.solver.ConstantTerm
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.Label.Companion.weakest
import io.github.apl_cornell.viaduct.security.Principal

/**
 * A stand-in for an unknown label. The solver will assign an actual value to each instance.
 *
 * @see ConstraintSolver.addNewVariable
 */
data class LabelVariable(
    public override val confidentialityComponent: AtomicTerm<FreeDistributiveLattice<Principal>>,
    public override val integrityComponent: AtomicTerm<FreeDistributiveLattice<Principal>>
) : AtomicLabelTerm() {
    override fun getValue(solution: ConstraintSolution): Label {
        return solution.getValue(this)
    }

    override fun confidentiality(): LabelVariable {
        return LabelVariable(confidentialityComponent, ConstantTerm(weakest.integrityComponent))
    }

    override fun integrity(): LabelVariable {
        return LabelVariable(ConstantTerm(weakest.confidentialityComponent), integrityComponent)
    }

    override fun swap(): AtomicLabelTerm {
        return LabelVariable(integrityComponent, confidentialityComponent)
    }

    override fun join(that: Label): LabelTerm {
        return ConstantJoinVariableTerm(that, this)
    }
}
