package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode

/**
 * A modular cost model for estimating the cost of executing a program given a protocol assignment.
 *
 * The cost model is modular because it is local: it cannot look at the entire program, instead,
 * it is restricted to viewing a single computation or a message at a time.
 */
// TODO: should protocols be nullable to take into account unassigned protocols?
interface CostEstimator<C : CostMonoid<C>> {
    /**
     * Estimated cost of running [computation] at [executingProtocol].
     */
    // TODO: not sure about taking in an expression. At the very least we needs methods too.
    fun executionCost(computation: ExpressionNode, executingProtocol: Protocol): Cost<C>

    /**
     * Estimated cost of sending a message of type [messageType] from [source] to [destination].
     */
    fun communicationCost(source: Protocol, destination: Protocol): Cost<C>

    /** Features that factor into cost estimation. */
    fun zeroCost(): Cost<C>

    /** Cost weights of features. */
    fun featureWeights(): Cost<C>
}
