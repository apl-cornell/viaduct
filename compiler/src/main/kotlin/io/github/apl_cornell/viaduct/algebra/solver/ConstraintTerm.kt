package io.github.apl_cornell.viaduct.algebra.solver

import io.github.apl_cornell.viaduct.algebra.HeytingAlgebra

/** Terms that appear in constraints. */
interface ConstraintTerm<A : HeytingAlgebra<A>> {
    /** Returns the value of this term given an assignment of values to every variable in the term. */
    fun getValue(solution: ConstraintSolution<A>): A

    /** Return the node that will represent this term in the constraint graph. */
    val node: AtomicTerm<A>
}
