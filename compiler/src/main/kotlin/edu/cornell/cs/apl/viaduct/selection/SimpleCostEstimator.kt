package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.protocols.ABY
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
                is ABY -> IntegerCost(100)
                else -> throw Error("unknown protocol ${executingProtocol.protocolName}")
            }
        )

    override fun communicationCost(source: Protocol, destination: Protocol): Cost<IntegerCost> {
        return if (source != destination) {
            val numMessages =
                SimpleProtocolComposer.communicate(source, destination).communicationMap.map { kv ->
                    val plaintextMsgCost =
                        kv.value.filter { event ->
                            event.send.host != event.recv.host && event.send.protocol !is ABY
                        }.size

                    val mpcExecCost =
                        if (kv.value.any { event -> event.send.protocol is ABY && event.recv.protocol !is ABY }) 10 else 0
                    plaintextMsgCost + mpcExecCost
                }.fold(0) { acc, phaseCost -> acc + phaseCost }

            zeroCost().update(NUM_MESSAGES, IntegerCost(numMessages))
        } else {
            zeroCost()
        }
    }

    override fun storageCost(declaration: ObjectDeclaration, protocol: Protocol): Cost<IntegerCost> =
        zeroCost().update(
            EXECUTION_COST,
            when (protocol) {
                is Local -> IntegerCost(1)
                is Replication -> IntegerCost(1)
                is ABY -> IntegerCost(100)
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
                NUM_MESSAGES to IntegerCost(1),
                BYTES_TRANSFERRED to IntegerCost(1),
                EXECUTION_COST to IntegerCost(1)
            )
        )
}
