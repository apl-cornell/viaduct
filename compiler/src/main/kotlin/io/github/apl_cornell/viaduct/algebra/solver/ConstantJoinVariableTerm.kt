package io.github.apl_cornell.viaduct.algebra.solver

import io.github.apl_cornell.viaduct.algebra.HeytingAlgebra
import io.github.apl_cornell.viaduct.util.dataflow.DataFlowEdge

/** Join of a constant element and a variable. */
internal data class ConstantJoinVariableTerm<A : HeytingAlgebra<A>>(val lhs: A, val rhs: VariableTerm<A>) :
    RightHandTerm<A> {
    override fun getValue(solution: ConstraintSolution<A>): A {
        return lhs.join(rhs.getValue(solution))
    }

    override val node: AtomicTerm<A>
        get() = rhs

    override val outEdge: DataFlowEdge<A>
        get() = JoinEdge(lhs)
}
