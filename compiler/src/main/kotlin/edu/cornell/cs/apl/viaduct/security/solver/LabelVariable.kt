package edu.cornell.cs.apl.viaduct.security.solver

import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice
import edu.cornell.cs.apl.viaduct.algebra.solver.AtomicTerm
import edu.cornell.cs.apl.viaduct.algebra.solver.ConstantTerm
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.Label.Companion.weakest
import edu.cornell.cs.apl.viaduct.security.Principal

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
