package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.ArithABY
import edu.cornell.cs.apl.viaduct.protocols.BoolABY
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.protocols.YaoABY
import edu.cornell.cs.apl.viaduct.syntax.Host
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
class SimpleCostEstimator(
    private val protocolComposer: ProtocolComposer
) : CostEstimator<IntegerCost> {
    companion object {
        private const val NUM_MESSAGES = "numberOfMessages"
        private const val BYTES_TRANSFERRED = "bytesTransferred"
        private const val EXECUTION_COST = "executionCost"
    }

    override fun executionCost(computation: ExpressionNode, protocol: Protocol): Cost<IntegerCost> =
        zeroCost().update(
            EXECUTION_COST,
            when (protocol) {
                is Local -> IntegerCost(1)
                is Replication -> IntegerCost(1)
                is Commitment -> IntegerCost(10)
                is ABY -> IntegerCost(100)
                else -> throw Error("unknown protocol ${protocol.protocolName}")
            }
        ).update(
            NUM_MESSAGES,
            when (protocol) {
                is ArithABY, is BoolABY -> IntegerCost(30)
                is YaoABY -> IntegerCost(10)
                else -> IntegerCost(0)
            }
        ).update(
            BYTES_TRANSFERRED,
            when (protocol) {
                is ArithABY, is BoolABY -> IntegerCost(10)
                is YaoABY -> IntegerCost(30)
                else -> IntegerCost(0)
            }
        )

    /** Cost of sending messages over the wire from one host to another. */
    private fun remoteMessagingCost(events: Iterable<CommunicationEvent>): Int =
        events.filter { event ->
            event.send.host != event.recv.host && event.send.protocol !is ABY &&
                event.send.id != Protocol.INTERNAL_OUTPUT
        }.size

    /** Cost of executing an MPC circuit, performed whenever communication from ABY
     * to another protocol occurs. */
    private fun mpcExecutionCost(events: List<CommunicationEvent>): Int =
        if (events.any { event ->
                event.send.protocol is ABY && event.send.id != Protocol.INTERNAL_OUTPUT && event.recv.protocol !is ABY }
        ) 10 else 0

    /** Cost of converting between different ABY circuit types. */
    // TODO: fill this in
    private fun abyShareConversionCost(events: List<CommunicationEvent>): Int =
        if (events.isNotEmpty()) 0 else events.size

    override fun communicationCost(source: Protocol, destination: Protocol, host: Host?): Cost<IntegerCost> {
        return if (source != destination) {
            val events =
                if (host != null) {
                    protocolComposer.communicate(source, destination).getHostReceives(host)
                } else {
                    protocolComposer.communicate(source, destination)
                }.filter { it.recv.id == Protocol.INTERNAL_INPUT || it.send.id == Protocol.INTERNAL_OUTPUT }

            val plaintextMsgCost = remoteMessagingCost(events)
            val mpcExecCost = mpcExecutionCost(events)
            val abyShareConversionCost = abyShareConversionCost(events)
            val messageCost = plaintextMsgCost + mpcExecCost + abyShareConversionCost

            zeroCost()
                .update(NUM_MESSAGES, IntegerCost(messageCost))
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
                is Commitment -> IntegerCost(10)
                is ABY -> IntegerCost(100)
                else -> IntegerCost(0)
            }
        ).update(
            NUM_MESSAGES,
            when (protocol) {
                is ArithABY, is BoolABY -> IntegerCost(30)
                is YaoABY -> IntegerCost(10)
                else -> IntegerCost(0)
            }
        ).update(
            BYTES_TRANSFERRED,
            when (protocol) {
                is ArithABY, is BoolABY -> IntegerCost(10)
                is YaoABY -> IntegerCost(30)
                else -> IntegerCost(0)
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

    /** Feature weights in low latency settings.
     *  Size of data transferred cost more than number of messages sent. */
    private val lowLatencyWeights: Cost<IntegerCost> =
        Cost(
            persistentMapOf(
                NUM_MESSAGES to IntegerCost(10),
                BYTES_TRANSFERRED to IntegerCost(15),
                EXECUTION_COST to IntegerCost(10)
            )
        )

    /** Feature weights in high latency settings.
     *  Number of messages sent cost more than size of data transferred. */
    private val highLatencyWeights: Cost<IntegerCost> =
        Cost(
            persistentMapOf(
                NUM_MESSAGES to IntegerCost(15),
                BYTES_TRANSFERRED to IntegerCost(10),
                EXECUTION_COST to IntegerCost(10)
            )
        )

    override fun featureWeights(): Cost<IntegerCost> = lowLatencyWeights
}
