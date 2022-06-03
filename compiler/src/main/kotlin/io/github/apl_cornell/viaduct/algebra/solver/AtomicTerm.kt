package io.github.apl_cornell.viaduct.algebra.solver

import io.github.apl_cornell.viaduct.algebra.HeytingAlgebra
import io.github.apl_cornell.viaduct.util.dataflow.DataFlowEdge
import io.github.apl_cornell.viaduct.util.dataflow.DataFlowNode
import io.github.apl_cornell.viaduct.util.dataflow.IdentityEdge

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
