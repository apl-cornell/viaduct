package io.github.aplcornell.viaduct.selection

import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.SimpleStatementNode

/**
 * A modular cost model for estimating the cost of executing a program given a protocol assignment.
 *
 * The cost model is modular because it is local: it cannot look at the entire program, instead,
 * it is restricted to viewing a single computation or a message at a time.
 */
interface CostEstimator<C : CostMonoid<C>> {
    /**
     * Estimated cost of executing [stmt] at [protocol].
     */
    fun executionCost(stmt: SimpleStatementNode, protocol: Protocol): Cost<C>

    /**
     * Estimated cost of sending a message from [source] to [destination] relative to [host] in [destination].
     * If [host] is null, then computes the cost for all the hosts in [destination].
     */
    fun communicationCost(source: Protocol, destination: Protocol, host: Host? = null): Cost<C>

    /** "Identity" cost. */
    fun zeroCost(): Cost<C>

    /** Cost weights of features. */
    fun featureWeights(): Cost<C>
}
