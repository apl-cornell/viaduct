package io.github.apl_cornell.viaduct.algebra.solver

import io.github.apl_cornell.viaduct.algebra.HeytingAlgebra

/**
 * A variable for the solver to find a value for.
 *
 * @see ConstraintSystem.addNewVariable
 * @constructor Creates a fresh variable.
 * @param label an arbitrary object to use as a label (useful for debugging)
 */
class VariableTerm<A : HeytingAlgebra<A>> internal constructor(private val label: Any) : AtomicTerm<A>() {
    override fun getValue(solution: ConstraintSolution<A>): A {
        return solution.getValue(this)
    }

    override fun meet(that: A): LeftHandTerm<A> {
        return ConstantMeetVariableTerm(that, this)
    }

    override fun join(that: A): RightHandTerm<A> {
        return ConstantJoinVariableTerm(that, this)
    }

    override fun transfer(input: A): A {
        return input
    }

    override fun toString(): String {
        return label.toString()
    }
}
