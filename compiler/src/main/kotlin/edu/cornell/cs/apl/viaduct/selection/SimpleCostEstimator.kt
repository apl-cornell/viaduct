package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.ArithABY
import edu.cornell.cs.apl.viaduct.protocols.BoolABY
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.protocols.YaoABY
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThan
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Negation
import edu.cornell.cs.apl.viaduct.syntax.operators.Subtraction
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
 *
 * costs for ABY mixed protocol derived from Table 2 in Ishaq et al, CCS 2019
 * - cost is from microsecond figure for non-amortized n=1, divided by 10 and rounded
 * */
class SimpleCostEstimator(
    private val protocolComposer: ProtocolComposer
) : CostEstimator<IntegerCost> {
    companion object {
        private const val NUM_MESSAGES = "numberOfMessages"
        private const val EXECUTION_COST = "executionCost"
        private const val LAN_COST = "lan"
        private const val WAN_COST = "wan"
    }

    private val mpcOperationCostMap: Map<Pair<Operator, ProtocolName>, Cost<IntegerCost>> =
        mapOf(
            // ADD
            Pair(Addition, ArithABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(9)).update(WAN_COST, IntegerCost(9)),

            Pair(Addition, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(20)).update(WAN_COST, IntegerCost(20)),

            Pair(Addition, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(15)).update(WAN_COST, IntegerCost(15)),

            // SUB
            Pair(Subtraction, ArithABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(9)).update(WAN_COST, IntegerCost(9)),

            Pair(Subtraction, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(445)).update(WAN_COST, IntegerCost(451)),

            Pair(Subtraction, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(149)).update(WAN_COST, IntegerCost(148)),

            // MUL
            Pair(Multiplication, ArithABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(306)).update(WAN_COST, IntegerCost(314)),

            Pair(Multiplication, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(583)).update(WAN_COST, IntegerCost(581)),

            Pair(Multiplication, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(281)).update(WAN_COST, IntegerCost(212)),

            // AND
            Pair(And, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(137)).update(WAN_COST, IntegerCost(137)),

            Pair(And, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(146)).update(WAN_COST, IntegerCost(145)),

            // OR
            Pair(And, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(138)).update(WAN_COST, IntegerCost(139)),

            Pair(And, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(146)).update(WAN_COST, IntegerCost(146)),

            // NOT (don't have numbers for these from Ishaq et al)
            Pair(Negation, BoolABY.protocolName) to zeroCost(),

            Pair(Negation, YaoABY.protocolName) to zeroCost(),

            // EQUAL TO
            Pair(EqualTo, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(184)).update(WAN_COST, IntegerCost(186)),

            Pair(EqualTo, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(146)).update(WAN_COST, IntegerCost(146)),

            // LESS THAN / EQUAL TO
            Pair(EqualTo, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(202)).update(WAN_COST, IntegerCost(202)),

            Pair(EqualTo, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(147)).update(WAN_COST, IntegerCost(147)),

            // LESS THAN
            Pair(LessThan, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(214)).update(WAN_COST, IntegerCost(214)),

            Pair(LessThan, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(148)).update(WAN_COST, IntegerCost(147)),

            // MUX
            Pair(Mux, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(141)).update(WAN_COST, IntegerCost(141)),

            Pair(Mux, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(148)).update(WAN_COST, IntegerCost(146)),

            // MIN = (MUX + LESS THAN)
            Pair(Minimum, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(141 + 214)).update(WAN_COST, IntegerCost(141 + 214)),

            Pair(Minimum, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(148 + 147)).update(WAN_COST, IntegerCost(146 + 147)),

            // MIN = (MUX + LESS THAN)
            Pair(Maximum, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(141)).update(WAN_COST, IntegerCost(141)),

            Pair(Maximum, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(148)).update(WAN_COST, IntegerCost(146))
        )

    override fun executionCost(stmt: SimpleStatementNode, protocol: Protocol): Cost<IntegerCost> =
        zeroCost().update(
            EXECUTION_COST,
            when (protocol) {
                is Local -> IntegerCost(1)
                is Replication -> IntegerCost(1)
                is Commitment -> IntegerCost(10)
                is ABY -> IntegerCost(100)
                else -> throw Error("unknown protocol ${protocol.protocolName}")
            }
        ).concat(
            when (protocol) {
                is ArithABY, is BoolABY, is YaoABY -> {
                    when (stmt) {
                        is LetNode -> {
                            when (val rhs = stmt.value) {
                                is OperatorApplicationNode ->
                                    mpcOperationCostMap[rhs.operator to protocol.protocolName]
                                        ?: throw Error("SimpleCostEstimator: no cost for operator ${rhs.operator} " +
                                            "in protocol ${protocol.protocolName}")

                                else -> zeroCost()
                            }
                        }

                        else -> zeroCost()
                    }
                }

                else -> zeroCost()
            }
        )

    /** Cost of sending messages over the wire from one host to another. */
    private fun remoteMessagingCost(events: Iterable<CommunicationEvent>): Int =
        events.filter { event ->
            event.send.host != event.recv.host && event.send.protocol !is ABY
        }.size

    /** Cost of executing an MPC circuit, performed whenever communication from ABY
     * to another protocol occurs. */
    private fun mpcExecutionCost(events: List<CommunicationEvent>): Int =
        if (events.any { event ->
                event.send.protocol is ABY && event.send.id != Protocol.INTERNAL_OUTPUT && event.recv.protocol !is ABY
            }
        ) 10 else 0

    private val costA2B: Cost<IntegerCost> =
        zeroCost().update(LAN_COST, IntegerCost(18)).update(WAN_COST, IntegerCost(18))

    private val costA2Y: Cost<IntegerCost> =
        zeroCost().update(LAN_COST, IntegerCost(17)).update(WAN_COST, IntegerCost(17))

    private val costB2A: Cost<IntegerCost> =
        zeroCost().update(LAN_COST, IntegerCost(14)).update(WAN_COST, IntegerCost(14))

    private val costB2Y: Cost<IntegerCost> =
        zeroCost().update(LAN_COST, IntegerCost(15)).update(WAN_COST, IntegerCost(15))

    private val costY2A: Cost<IntegerCost> =
        zeroCost().update(LAN_COST, IntegerCost(20)).update(WAN_COST, IntegerCost(20))

    private val costY2B: Cost<IntegerCost> =
        zeroCost().update(LAN_COST, IntegerCost(15)).update(WAN_COST, IntegerCost(15))

    /** Cost of converting between different ABY circuit types. */
    private fun abyShareConversionCost(events: List<CommunicationEvent>): Cost<IntegerCost> {
        var conversionCost: Cost<IntegerCost> = zeroCost()
        var hasA2B = false
        var hasA2Y = false
        var hasB2A = false
        var hasB2Y = false
        var hasY2A = false
        var hasY2B = false
        for (event in events) {
            when {
                event.send.id == ArithABY.A2B_OUTPUT && !hasA2B -> {
                    conversionCost = conversionCost.concat(costA2B)
                    hasA2B = true
                }

                event.send.id == ArithABY.A2Y_OUTPUT && !hasA2Y -> {
                    conversionCost = conversionCost.concat(costA2Y)
                    hasA2Y = true
                }

                event.send.id == BoolABY.B2A_OUTPUT && !hasB2A -> {
                    conversionCost = conversionCost.concat(costB2A)
                    hasB2A = true
                }

                event.send.id == BoolABY.B2Y_OUTPUT && !hasB2Y -> {
                    conversionCost = conversionCost.concat(costB2Y)
                    hasB2Y = true
                }

                event.send.id == YaoABY.Y2A_OUTPUT && !hasY2A -> {
                    conversionCost = conversionCost.concat(costY2A)
                    hasY2A = true
                }

                event.send.id == YaoABY.Y2B_OUTPUT && !hasY2B -> {
                    conversionCost = conversionCost.concat(costY2B)
                    hasY2B = true
                }
            }
        }

        return conversionCost
    }

    override fun communicationCost(source: Protocol, destination: Protocol, host: Host?): Cost<IntegerCost> {
        return if (source != destination) {
            val events =
                if (host != null) {
                    protocolComposer.communicate(source, destination).getHostReceives(host)
                } else {
                    protocolComposer.communicate(source, destination)
                }.filter { it.recv.id != Protocol.INTERNAL_INPUT || it.send.id != Protocol.INTERNAL_OUTPUT }

            val plaintextMsgCost = remoteMessagingCost(events)
            val mpcExecCost = mpcExecutionCost(events)
            val messageCost = plaintextMsgCost + mpcExecCost

            val abyShareConversionCost = abyShareConversionCost(events)

            zeroCost()
                .update(NUM_MESSAGES, IntegerCost(messageCost))
                .concat(abyShareConversionCost)
        } else {
            zeroCost()
        }
    }

    override fun zeroCost(): Cost<IntegerCost> =
        Cost(
            persistentMapOf(
                NUM_MESSAGES to IntegerCost(0),
                EXECUTION_COST to IntegerCost(0),
                LAN_COST to IntegerCost(0),
                WAN_COST to IntegerCost(0)
            )
        )

    /** Feature weights in low latency settings.
     *  Size of data transferred cost more than number of messages sent. */
    private val lanWeights: Cost<IntegerCost> =
        Cost(
            persistentMapOf(
                NUM_MESSAGES to IntegerCost(1),
                EXECUTION_COST to IntegerCost(1),
                LAN_COST to IntegerCost(1),
                WAN_COST to IntegerCost(0)
            )
        )

    /** Feature weights in high latency settings.
     *  Number of messages sent cost more than size of data transferred. */
    private val wanWeights: Cost<IntegerCost> =
        Cost(
            persistentMapOf(
                NUM_MESSAGES to IntegerCost(1),
                EXECUTION_COST to IntegerCost(1),
                LAN_COST to IntegerCost(0),
                WAN_COST to IntegerCost(1)
            )
        )

    override fun featureWeights(): Cost<IntegerCost> = lanWeights
}
