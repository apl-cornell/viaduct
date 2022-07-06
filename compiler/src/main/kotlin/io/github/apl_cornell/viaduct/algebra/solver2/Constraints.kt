package io.github.apl_cornell.viaduct.algebra.solver2

import io.github.apl_cornell.viaduct.algebra.HeytingAlgebra
import io.github.apl_cornell.viaduct.algebra.JoinSemiLattice
import io.github.apl_cornell.viaduct.util.dataflow.DataFlowEdge

/** A lattice constraint. */
sealed class Constraint<C, V, T>

/** A constraint that represents `[from] flowsTo [to]`. */
internal data class FlowsToConstraint<C, V, T>(
    val from: Term<C, V>,
    val to: Term<C, V>,
    val failWith: (from: C, to: C) -> T,
    internal val reducedForm: Iterable<ReducedFlowsToConstraint<C, V>>
) : Constraint<C, V, T>()

/** A constraint that represents `[from] flowsTo [propagate].propagate([to])`. */
internal data class ReducedFlowsToConstraint<C, V>(
    val from: AtomicTerm<C, V>,
    val propagate: DataFlowEdge<C>,
    val to: AtomicTerm<C, V>
)

/** [DataFlowEdge]s that appear on the left-hand of [LeftHandEdge.compose]. */
internal sealed interface LeftHandEdge<C> {
    /** Maps `input` to `this(that(input))`.*/
    fun compose(that: RightHandEdge<C>): DataFlowEdge<C>
}

/** [DataFlowEdge]s that appear on the right-hand of [LeftHandEdge.compose]. */
internal sealed interface RightHandEdge<C> : DataFlowEdge<C>

/** Maps `input` to `input`. */
internal class IdentityEdge<C> : LeftHandEdge<C>, RightHandEdge<C> {
    override fun propagate(input: C): C = input

    override fun compose(that: RightHandEdge<C>): DataFlowEdge<C> = that

    override fun toString(): String = ""
}

/** Maps `input` to `[antecedent] → input`. */
internal class ImplyEdge<C : HeytingAlgebra<C>>(val antecedent: C) : LeftHandEdge<C>, DataFlowEdge<C> {
    override fun propagate(input: C): C = antecedent imply input

    override fun compose(that: RightHandEdge<C>): DataFlowEdge<C> =
        when (that) {
            is IdentityEdge ->
                this
            is JoinEdge ->
                ImplyJoinEdge(this.antecedent, that.joined)
        }

    override fun toString(): String = "$antecedent → _"
}

/** Maps `input` to `[joined] ∨ input`. */
internal class JoinEdge<C : JoinSemiLattice<C>>(val joined: C) : RightHandEdge<C> {
    override fun propagate(input: C): C = joined join input

    override fun toString(): String = "$joined ∨ _"
}

/** Maps `input` to `[antecedent] → ([joined] ∨ input)`. */
internal class ImplyJoinEdge<C : HeytingAlgebra<C>>(val antecedent: C, val joined: C) : DataFlowEdge<C> {
    override fun propagate(input: C): C = antecedent imply (joined join input)

    override fun toString(): String = "$antecedent → ($joined ∨ _)"
}
