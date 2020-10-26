package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclaration

/**
 * A modular cost model for estimating the cost of executing a program given a protocol assignment.
 *
 * The cost model is modular because it is local: it cannot look at the entire program, instead,
 * it is restricted to viewing a single computation or a message at a time.
 */
interface CostEstimator<C : CostMonoid<C>> {
    /**
     * Estimated cost of running [computation] at [protocol].
     */
    // TODO: not sure about taking in an expression. At the very least we needs methods too.
    fun executionCost(computation: ExpressionNode, protocol: Protocol): Cost<C>

    /**
     * Estimated cost of sending a message of type [messageType] from [source] to [destination]
     * relative to [host] in [destination]. If [host] is null, then computes the cost for
     * all the hosts in [destination].
     */
    fun communicationCost(source: Protocol, destination: Protocol, host: Host? = null): Cost<C>

    /** Estimated cost of storing object defined by [declaration] in protocol [protocol]. */
    fun storageCost(declaration: ObjectDeclaration, protocol: Protocol): Cost<C>

    /** "Identity" cost. */
    fun zeroCost(): Cost<C>

    /** Cost weights of features. */
    fun featureWeights(): Cost<C>
}
