package io.github.apl_cornell.viaduct.algebra.solver2

import io.github.apl_cornell.viaduct.algebra.Lattice

/** A solution to a system of constraints. Maps variables of type [V] to values of type [C]. */
class ConstraintSolution<C : Lattice<C>, V> internal constructor(
    private val solution: Map<V, C>,
    private val default: C
) : (V) -> C {
    /** Returns the value of [term]. */
    fun evaluate(term: Term<C, V>): C =
        when (term) {
            is Constant -> term.value
            is Variable -> this(term.value)
            is Join -> evaluate(term.lhs) join evaluate(term.rhs)
            is Meet -> evaluate(term.lhs) meet evaluate(term.rhs)
        }

    override fun invoke(variable: V): C =
        solution.getOrDefault(variable, default)
}
