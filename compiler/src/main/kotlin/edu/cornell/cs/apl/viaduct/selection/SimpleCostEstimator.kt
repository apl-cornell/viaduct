package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.ArithABY
import edu.cornell.cs.apl.viaduct.protocols.BoolABY
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.protocols.YaoABY
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.Division
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.ExclusiveOr
import edu.cornell.cs.apl.viaduct.syntax.operators.GreaterThan
import edu.cornell.cs.apl.viaduct.syntax.operators.GreaterThanOrEqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThan
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThanOrEqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Negation
import edu.cornell.cs.apl.viaduct.syntax.operators.Subtraction
import kotlinx.collections.immutable.persistentMapOf

enum class SimpleCostRegime { LAN, WAN }

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
    private val protocolComposer: ProtocolComposer,
    private val costRegime: SimpleCostRegime
) : CostEstimator<IntegerCost> {
    companion object {
        private const val NUM_MESSAGES = "numberOfMessages"
        private const val EXECUTION_COST = "executionCost"
        private const val LAN_COST = "lan"
        private const val WAN_COST = "wan"
    }

    private fun opCost(lan: Int, wan: Int) =
        zeroCost().update(LAN_COST, IntegerCost(lan)).update(WAN_COST, IntegerCost(wan))

    // from Ishaq et al, CCS 2019
    // numbers from Table 2, n=1, w/ the microsecond figure divided by 100
    private val mpcOperationCostMap: Map<Pair<Operator, ProtocolName>, Cost<IntegerCost>> =
        mapOf(
            // ADD
            Pair(Addition, ArithABY.protocolName) to opCost(9, 9),
            Pair(Addition, BoolABY.protocolName) to opCost(20, 20),
            Pair(Addition, YaoABY.protocolName) to opCost(15, 15),

            // SUB
            Pair(Subtraction, ArithABY.protocolName) to opCost(9, 9),
            Pair(Subtraction, BoolABY.protocolName) to opCost(445, 451),
            Pair(Subtraction, YaoABY.protocolName) to opCost(149, 148),

            // NEGATION (treat like subtraction)
            Pair(Negation, ArithABY.protocolName) to opCost(9, 9),
            Pair(Negation, BoolABY.protocolName) to opCost(445, 451),
            Pair(Negation, YaoABY.protocolName) to opCost(149, 148),

            // MUL
            Pair(Multiplication, ArithABY.protocolName) to opCost(306, 314),
            Pair(Multiplication, BoolABY.protocolName) to opCost(583, 581),
            Pair(Multiplication, YaoABY.protocolName) to opCost(281, 212),

            // DIV (copied from MUL), TODO: fix this
            Pair(Division, BoolABY.protocolName) to opCost(583, 581),
            Pair(Division, YaoABY.protocolName) to opCost(281, 212),

            // AND
            Pair(And, BoolABY.protocolName) to opCost(137, 137),
            Pair(And, YaoABY.protocolName) to opCost(146, 145),

            // OR
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Or, BoolABY.protocolName) to
                opCost(138, 139),
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Or, YaoABY.protocolName) to
                opCost(146, 146),

            // NOT (don't have numbers for these, copy AND)
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Not, BoolABY.protocolName) to opCost(137, 137),
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Not, YaoABY.protocolName) to opCost(146, 145),

            // EQUAL TO
            Pair(EqualTo, BoolABY.protocolName) to opCost(184, 186),
            Pair(EqualTo, YaoABY.protocolName) to opCost(146, 146),

            // LESS THAN / EQUAL TO
            Pair(LessThanOrEqualTo, BoolABY.protocolName) to opCost(202, 202),
            Pair(LessThanOrEqualTo, YaoABY.protocolName) to opCost(147, 147),

            // LESS THAN
            Pair(LessThan, BoolABY.protocolName) to opCost(214, 214),
            Pair(LessThan, YaoABY.protocolName) to opCost(148, 147),

            // GREATER THAN / EQUAL TO
            Pair(GreaterThanOrEqualTo, BoolABY.protocolName) to opCost(202, 202),
            Pair(GreaterThanOrEqualTo, YaoABY.protocolName) to opCost(147, 147),

            // GREATER THAN
            Pair(GreaterThan, BoolABY.protocolName) to opCost(214, 214),
            Pair(GreaterThan, YaoABY.protocolName) to opCost(148, 147),

            // MUX
            Pair(Mux, BoolABY.protocolName) to opCost(141, 141),
            Pair(Mux, YaoABY.protocolName) to opCost(148, 146),

            // MIN = (MUX + LESS THAN)
            Pair(Minimum, BoolABY.protocolName) to opCost(141 + 214, 141 + 214),
            Pair(Minimum, YaoABY.protocolName) to opCost(148 + 147, 146 + 147),

            // MIN = (MUX + LESS THAN)
            Pair(Maximum, BoolABY.protocolName) to opCost(141, 141),
            Pair(Maximum, YaoABY.protocolName) to opCost(148, 146)
        )

    // from the original ABY paper; cost is from setup + (sequential) online time divided by 100
    private val mpcOperationCostMap2: Map<Pair<Operator, ProtocolName>, Cost<IntegerCost>> =
        mapOf(
            // ADD
            Pair(Addition, ArithABY.protocolName) to opCost(0, 0),
            Pair(Addition, BoolABY.protocolName) to opCost(20, 20),
            Pair(Addition, YaoABY.protocolName) to opCost(1, 24),

            // SUB (TODO: not in table, copied ADD)
            Pair(Subtraction, ArithABY.protocolName) to opCost(0, 0),
            Pair(Subtraction, BoolABY.protocolName) to opCost(20, 20),
            Pair(Subtraction, YaoABY.protocolName) to opCost(1, 24),

            // NEGATION (treat like subtraction)
            Pair(Negation, ArithABY.protocolName) to opCost(0, 0),
            Pair(Negation, BoolABY.protocolName) to opCost(20, 20),
            Pair(Negation, YaoABY.protocolName) to opCost(1, 24),

            // MUL
            Pair(Multiplication, ArithABY.protocolName) to opCost(2, 2391),
            Pair(Multiplication, BoolABY.protocolName) to opCost(103, 67455),
            Pair(Multiplication, YaoABY.protocolName) to opCost(281, 212),

            // DIV (TODO: no numbers for these, copied from MUL)
            Pair(Division, BoolABY.protocolName) to opCost(103, 67455),
            Pair(Division, YaoABY.protocolName) to opCost(281, 212),

            // AND
            Pair(And, BoolABY.protocolName) to opCost(1, 1932),
            Pair(And, YaoABY.protocolName) to opCost(0, 23),

            // OR (TODO: no numbers for these, copy AND)
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Or, BoolABY.protocolName) to
                opCost(1, 1932),
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Or, YaoABY.protocolName) to
                opCost(0, 23),

            // NOT (TODO: no numbers for these, copy AND)
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Not, BoolABY.protocolName) to opCost(1, 1932),
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Not, YaoABY.protocolName) to opCost(0, 23),

            Pair(ExclusiveOr, BoolABY.protocolName) to opCost(0, 0),
            Pair(ExclusiveOr, YaoABY.protocolName) to opCost(0, 12),

            // EQUAL TO
            Pair(EqualTo, BoolABY.protocolName) to opCost(6, 11300),
            Pair(EqualTo, YaoABY.protocolName) to opCost(1, 23),

            // LESS THAN / EQUAL TO (cost given as CMP)
            Pair(LessThanOrEqualTo, BoolABY.protocolName) to opCost(8, 10802),
            Pair(LessThanOrEqualTo, YaoABY.protocolName) to opCost(1, 23),

            // LESS THAN (cost given as CMP)
            Pair(LessThan, BoolABY.protocolName) to opCost(8, 10802),
            Pair(LessThan, YaoABY.protocolName) to opCost(1, 23),

            // GREATER THAN / EQUAL TO (cost given as CMP)
            Pair(GreaterThanOrEqualTo, BoolABY.protocolName) to opCost(8, 10802),
            Pair(GreaterThanOrEqualTo, YaoABY.protocolName) to opCost(1, 23),

            // GREATER THAN (cost given as CMP)
            Pair(GreaterThan, BoolABY.protocolName) to opCost(8, 10802),
            Pair(GreaterThan, YaoABY.protocolName) to opCost(1, 23),

            // MUX
            Pair(Mux, BoolABY.protocolName) to opCost(2, 2252),
            Pair(Mux, YaoABY.protocolName) to opCost(1, 23),

            // MIN = (MUX + LESS THAN)
            Pair(Minimum, BoolABY.protocolName) to opCost(2 + 8, 2252 + 10802),
            Pair(Minimum, YaoABY.protocolName) to opCost(1 + 1, 23 + 23),

            // MIN = (MUX + LESS THAN)
            Pair(Maximum, BoolABY.protocolName) to opCost(2 + 8, 2252 + 10802),
            Pair(Maximum, YaoABY.protocolName) to opCost(1 + 1, 23 + 23)
        )

    // from my own estimation
    // TODO: finish weights for WAN
    private val mpcOperationCostMap3: Map<Pair<Operator, ProtocolName>, Cost<IntegerCost>> =
        mapOf(
            // ADD
            Pair(Addition, ArithABY.protocolName) to opCost(4, 4),
            Pair(Addition, BoolABY.protocolName) to opCost(24, 24),
            Pair(Addition, YaoABY.protocolName) to opCost(17, 17),

            // SUB
            Pair(Subtraction, ArithABY.protocolName) to opCost(5, 5),
            Pair(Subtraction, BoolABY.protocolName) to opCost(60, 60),
            Pair(Subtraction, YaoABY.protocolName) to opCost(16, 16),

            // NEGATION (treat like subtraction)
            Pair(Negation, ArithABY.protocolName) to opCost(5, 5),
            Pair(Negation, BoolABY.protocolName) to opCost(60, 60),
            Pair(Negation, YaoABY.protocolName) to opCost(16, 16),

            // MUL
            Pair(Multiplication, ArithABY.protocolName) to opCost(17, 17),
            Pair(Multiplication, BoolABY.protocolName) to opCost(80, 80),
            Pair(Multiplication, YaoABY.protocolName) to opCost(46, 46),

            // DIV
            Pair(Division, BoolABY.protocolName) to opCost(378, 378),
            Pair(Division, YaoABY.protocolName) to opCost(130, 130),

            // AND
            Pair(And, BoolABY.protocolName) to opCost(20, 20),
            Pair(And, YaoABY.protocolName) to opCost(22, 22),

            // OR = NOT (AND (NOT lhs) (NOT rhs))
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Or, BoolABY.protocolName) to
                opCost(20 + (3 * 5), 20 + (3 * 5)),
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Or, YaoABY.protocolName) to
                opCost(22 + (3 * 6), 22 + (3 * 6)),

            // NOT
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Not, BoolABY.protocolName) to opCost(5, 5),
            Pair(edu.cornell.cs.apl.viaduct.syntax.operators.Not, YaoABY.protocolName) to opCost(6, 6),

            // EQUAL TO
            Pair(EqualTo, BoolABY.protocolName) to opCost(25, 25),
            Pair(EqualTo, YaoABY.protocolName) to opCost(18, 18),

            // LESS THAN / EQUAL TO = GT + OR + EQ
            Pair(LessThanOrEqualTo, BoolABY.protocolName) to opCost(26 + 25 + 20 + (3 * 5), 26 + 25 + 20 + (3 * 5)),
            Pair(LessThanOrEqualTo, YaoABY.protocolName) to opCost(18 + 18 + 22 + (3 * 6), 18 + 18 + 22 + (3 * 6)),

            // LESS THAN = GT
            Pair(LessThan, BoolABY.protocolName) to opCost(26, 26),
            Pair(LessThan, YaoABY.protocolName) to opCost(18, 18),

            // GREATER THAN / EQUAL TO = GT + OR + EQ
            Pair(GreaterThanOrEqualTo, BoolABY.protocolName) to opCost(26 + 25 + 20 + (3 * 5), 26 + 25 + 20 + (3 * 5)),
            Pair(GreaterThanOrEqualTo, YaoABY.protocolName) to opCost(18 + 18 + 22 + (3 * 6), 18 + 18 + 22 + (3 * 6)),

            // GREATER THAN = GT
            Pair(GreaterThan, BoolABY.protocolName) to opCost(26, 26),
            Pair(GreaterThan, YaoABY.protocolName) to opCost(18, 18),

            // MUX
            Pair(Mux, BoolABY.protocolName) to opCost(14, 14),
            Pair(Mux, YaoABY.protocolName) to opCost(9, 9),

            // MIN
            Pair(Minimum, BoolABY.protocolName) to opCost(35, 35),
            Pair(Minimum, YaoABY.protocolName) to opCost(19, 19),

            // MAX
            Pair(Maximum, BoolABY.protocolName) to opCost(34, 34),
            Pair(Maximum, YaoABY.protocolName) to opCost(18, 18)
        )

    override fun executionCost(stmt: SimpleStatementNode, protocol: Protocol): Cost<IntegerCost> =
        zeroCost().update(
            EXECUTION_COST,
            when (protocol) {
                is Local -> IntegerCost(1)
                is Replication -> IntegerCost(1)
                is Commitment -> IntegerCost(10)
                is ZKP -> IntegerCost(20)
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
                                    mpcOperationCostMap3[rhs.operator to protocol.protocolName]
                                        ?: throw Error(
                                            "SimpleCostEstimator: no cost for operator ${rhs.operator} " +
                                                "in protocol ${protocol.protocolName}"
                                        )

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

    // from Ishaq et al CCS 2019
    private val abyConversionCostMap: Map<Pair<ProtocolName, ProtocolName>, Cost<IntegerCost>> =
        mapOf(
            Pair(ArithABY.protocolName, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(18)).update(WAN_COST, IntegerCost(18)),

            Pair(ArithABY.protocolName, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(17)).update(WAN_COST, IntegerCost(17)),

            Pair(BoolABY.protocolName, ArithABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(14)).update(WAN_COST, IntegerCost(14)),

            Pair(BoolABY.protocolName, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(15)).update(WAN_COST, IntegerCost(15)),

            Pair(YaoABY.protocolName, ArithABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(20)).update(WAN_COST, IntegerCost(20)),

            Pair(YaoABY.protocolName, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(1)).update(WAN_COST, IntegerCost(15))
        )

    // from ABY paper
    private val abyConversionCostMap2: Map<Pair<ProtocolName, ProtocolName>, Cost<IntegerCost>> =
        mapOf(
            Pair(ArithABY.protocolName, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(1)).update(WAN_COST, IntegerCost(4346)),

            Pair(ArithABY.protocolName, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(1)).update(WAN_COST, IntegerCost(4346)),

            Pair(BoolABY.protocolName, ArithABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(0)).update(WAN_COST, IntegerCost(4191)),

            Pair(BoolABY.protocolName, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(0)).update(WAN_COST, IntegerCost(4790)),

            Pair(YaoABY.protocolName, ArithABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(0)).update(WAN_COST, IntegerCost(4191)),

            Pair(YaoABY.protocolName, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(0)).update(WAN_COST, IntegerCost(0))
        )

    // from my estimation
    // TODO: add WAN
    private val abyConversionCostMap3: Map<Pair<ProtocolName, ProtocolName>, Cost<IntegerCost>> =
        mapOf(
            Pair(ArithABY.protocolName, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(19)).update(WAN_COST, IntegerCost(19)),

            Pair(ArithABY.protocolName, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(18)).update(WAN_COST, IntegerCost(18)),

            Pair(BoolABY.protocolName, ArithABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(15)).update(WAN_COST, IntegerCost(15)),

            Pair(BoolABY.protocolName, YaoABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(16)).update(WAN_COST, IntegerCost(16)),

            Pair(YaoABY.protocolName, ArithABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(15)).update(WAN_COST, IntegerCost(15)),

            Pair(YaoABY.protocolName, BoolABY.protocolName) to
                zeroCost().update(LAN_COST, IntegerCost(5)).update(WAN_COST, IntegerCost(5))
        )

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
                    conversionCost =
                        conversionCost.concat(abyConversionCostMap3[ArithABY.protocolName to BoolABY.protocolName]!!)
                    hasA2B = true
                }

                event.send.id == ArithABY.A2Y_OUTPUT && !hasA2Y -> {
                    conversionCost =
                        conversionCost.concat(abyConversionCostMap3[ArithABY.protocolName to YaoABY.protocolName]!!)
                    hasA2Y = true
                }

                event.send.id == BoolABY.B2A_OUTPUT && !hasB2A -> {
                    conversionCost =
                        conversionCost.concat(abyConversionCostMap3[BoolABY.protocolName to ArithABY.protocolName]!!)
                    hasB2A = true
                }

                event.send.id == BoolABY.B2Y_OUTPUT && !hasB2Y -> {
                    conversionCost =
                        conversionCost.concat(abyConversionCostMap3[BoolABY.protocolName to YaoABY.protocolName]!!)
                    hasB2Y = true
                }

                event.send.id == YaoABY.Y2A_OUTPUT && !hasY2A -> {
                    conversionCost =
                        conversionCost.concat(abyConversionCostMap3[YaoABY.protocolName to ArithABY.protocolName]!!)
                    hasY2A = true
                }

                event.send.id == YaoABY.Y2B_OUTPUT && !hasY2B -> {
                    conversionCost =
                        conversionCost.concat(abyConversionCostMap3[YaoABY.protocolName to BoolABY.protocolName]!!)
                    hasY2B = true
                }
            }
        }

        return conversionCost
    }

    override fun communicationCost(source: Protocol, destination: Protocol, host: Host?): Cost<IntegerCost> {
        return if (source != destination && protocolComposer.canCommunicate(source, destination)) {
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

    override fun featureWeights(): Cost<IntegerCost> =
        when (costRegime) {
            SimpleCostRegime.LAN -> lanWeights
            SimpleCostRegime.WAN -> wanWeights
        }
}
