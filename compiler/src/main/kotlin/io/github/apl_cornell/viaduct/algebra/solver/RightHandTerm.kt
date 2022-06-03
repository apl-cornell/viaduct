package io.github.apl_cornell.viaduct.algebra.solver

import io.github.apl_cornell.viaduct.algebra.HeytingAlgebra
import io.github.apl_cornell.viaduct.util.dataflow.DataFlowEdge

/** Terms that can appear on the right-hand side of constraints. */
interface RightHandTerm<A : HeytingAlgebra<A>> : ConstraintTerm<A> {
    /**
     * Return an edge that captures the operation preformed by this term. In the constraint graph,
     * this will become an outgoing edge of [node].
     */
    val outEdge: DataFlowEdge<A>
}
