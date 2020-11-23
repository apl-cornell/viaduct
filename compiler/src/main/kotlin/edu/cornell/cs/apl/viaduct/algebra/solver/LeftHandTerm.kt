package edu.cornell.cs.apl.viaduct.algebra.solver

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge

/** Terms that can appear on the left-hand side of constraints. */
interface LeftHandTerm<A : HeytingAlgebra<A>> : ConstraintTerm<A> {
    /**
     * Return an edge that captures the operation preformed by this term. In the constraint graph,
     * this will become an incoming edge of [node].
     */
    val inEdge: DataFlowEdge<A>
}
