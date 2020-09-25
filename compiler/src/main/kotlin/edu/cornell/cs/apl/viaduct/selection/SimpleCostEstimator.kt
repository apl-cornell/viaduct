package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclaration
import kotlinx.collections.immutable.persistentMapOf

/**
 * Cost estimator for Local, Replication and ABY protocols.
 *
 * Approximates two communication features:
 * - NUM_MESSAGES: number of messages exchanged between hosts
 * - BYTES_TRANSFERRED: size of data transferred between hosts
 *
 * in high latency regimes, NUM_MESSAGES is weighted more;
 * in low latency regimes, BYTES_TRANSFERRED is weighted more.
 * */
object SimpleCostEstimator : CostEstimator<IntegerCost> {
    private const val NUM_MESSAGES = "numberOfMessages"
    private const val BYTES_TRANSFERRED = "bytesTransferred"
    private const val EXECUTION_COST = "executionCost"

    override fun executionCost(computation: ExpressionNode, executingProtocol: Protocol): Cost<IntegerCost> =
        zeroCost().update(
            EXECUTION_COST,
            when (executingProtocol) {
                is Local -> IntegerCost(1)
                is Replication -> IntegerCost(1)
                is Commitment -> IntegerCost(1)
                is ABY -> IntegerCost(10)
                else -> throw Error("unknown protocol ${executingProtocol.protocolName}")
            }
        )

    override fun communicationCost(source: Protocol, destination: Protocol): Cost<IntegerCost> =
        when {
            source is Local && destination is Local -> {
                zeroCost().update(
                    NUM_MESSAGES,
                    IntegerCost(if (source.host == destination.host) 0 else 1)
                )
            }

            source is Local && destination is Replication -> {
                zeroCost().update(
                    NUM_MESSAGES,
                    IntegerCost(destination.hosts.size)
                )
            }

            source is Local && destination is ABY -> {
                zeroCost().update(
                    NUM_MESSAGES,
                    IntegerCost(if (destination.hosts.contains(source.host)) 0 else destination.hosts.size)
                )
            }

            source is Replication && destination is Local -> {
                zeroCost().update(
                    NUM_MESSAGES,
                    IntegerCost(if (source.hosts.contains(destination.host)) 0 else source.hosts.size)
                )
            }

            // TODO: check if this is right
            source is Replication && destination is Replication -> {
                val destHostsComplement = destination.hosts.removeAll(source.hosts)
                zeroCost().update(
                    NUM_MESSAGES,
                    IntegerCost(destHostsComplement.size * source.hosts.size)
                )
            }

            // TODO: check if this is right
            source is Replication && destination is ABY -> {
                val destHostsComplement = destination.hosts.removeAll(source.hosts)
                zeroCost().update(
                    NUM_MESSAGES,
                    IntegerCost(destHostsComplement.size * source.hosts.size)
                )
            }

            source is ABY && destination is Local -> {
                zeroCost().update(
                    NUM_MESSAGES,
                    IntegerCost(if (source.hosts.contains(destination.host)) 0 else source.hosts.size)
                )
            }

            source is ABY && destination is Replication -> {
                val destHostsComplement = destination.hosts.removeAll(source.hosts)
                zeroCost().update(
                    NUM_MESSAGES,
                    IntegerCost(destHostsComplement.size * source.hosts.size)
                )
            }

            source is ABY && destination is ABY -> zeroCost()

            source is Commitment -> zeroCost() // TODO
            destination is Commitment -> zeroCost() // TODO

            else -> throw Error("unknown source protocol ${source.protocolName} or destination protocol ${destination.protocolName}")
        }

    override fun storageCost(declaration: ObjectDeclaration, protocol: Protocol): Cost<IntegerCost> =
        zeroCost().update(
            EXECUTION_COST,
            when (protocol) {
                is Local -> IntegerCost(1)
                is Replication -> IntegerCost(1)
                is Commitment -> IntegerCost(1)
                is ABY -> IntegerCost(10)
                else -> throw Error("unknown protocol ${protocol.protocolName}")
            }
        )

    override fun zeroCost(): Cost<IntegerCost> =
        Cost(
            persistentMapOf(
                NUM_MESSAGES to IntegerCost(0),
                BYTES_TRANSFERRED to IntegerCost(0),
                EXECUTION_COST to IntegerCost(0)
            )
        )

    override fun featureWeights(): Cost<IntegerCost> =
        Cost(
            persistentMapOf(
                NUM_MESSAGES to IntegerCost(5),
                BYTES_TRANSFERRED to IntegerCost(5),
                EXECUTION_COST to IntegerCost(1)
            )
        )
}
