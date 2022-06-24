package edu.cornell.cs.apl.viaduct.algebra.solver

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowNode
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge

/**
 * Constraint terms that are fully evaluated. For example, constants and variables, but not
 * expressions.
 */
abstract class AtomicTerm<A : HeytingAlgebra<A>> : LeftHandTerm<A>, RightHandTerm<A>, DataFlowNode<A> {
    /** Return a term that represents the meet of `this` with a constant. */
    abstract fun meet(that: A): LeftHandTerm<A>

    /** Return a term that represents the join of `this` with a constant. */
    abstract fun join(that: A): RightHandTerm<A>

    override val node: AtomicTerm<A>
        get() = this

    override val outEdge: DataFlowEdge<A>
        get() = IdentityEdge()

    override val inEdge: DataFlowEdge<A>
        get() = IdentityEdge()
}
