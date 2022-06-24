package io.github.apl_cornell.viaduct.algebra.solver

import io.github.apl_cornell.viaduct.algebra.HeytingAlgebra
import io.github.apl_cornell.viaduct.util.dataflow.DataFlowEdge

/** Meet of a constant element and a variable. */
internal data class ConstantMeetVariableTerm<A : HeytingAlgebra<A>>(val lhs: A, val rhs: VariableTerm<A>) :
    LeftHandTerm<A> {
    override fun getValue(solution: ConstraintSolution<A>): A {
        return lhs.meet(rhs.getValue(solution))
    }

    override val node: AtomicTerm<A>
        get() = rhs

    override val inEdge: DataFlowEdge<A>
        get() = ImplyEdge(lhs)
}
