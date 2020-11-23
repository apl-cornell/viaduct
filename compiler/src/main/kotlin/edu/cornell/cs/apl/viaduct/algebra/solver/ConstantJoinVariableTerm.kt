package edu.cornell.cs.apl.viaduct.algebra.solver

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge

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
