package edu.cornell.cs.apl.viaduct.algebra.solver

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra

/** Term representing a constant element. */
data class ConstantTerm<A : HeytingAlgebra<A>>(val value: A) : AtomicTerm<A>() {
    override fun getValue(solution: ConstraintSolution<A>): A {
        return value
    }

    override fun meet(that: A): ConstantTerm<A> {
        return ConstantTerm(value.meet(that))
    }

    override fun join(that: A): ConstantTerm<A> {
        return ConstantTerm(value.join(that))
    }

    override fun transfer(input: A): A {
        return value
    }

    override fun toString(): String {
        return value.toString()
    }
}
