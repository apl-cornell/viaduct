package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import de.tu_darmstadt.cs.encrypto.aby.ABYParty
import de.tu_darmstadt.cs.encrypto.aby.Aby
import de.tu_darmstadt.cs.encrypto.aby.Circuit
import de.tu_darmstadt.cs.encrypto.aby.Phase
import de.tu_darmstadt.cs.encrypto.aby.Role
import de.tu_darmstadt.cs.encrypto.aby.Share
import de.tu_darmstadt.cs.encrypto.aby.SharingType
import edu.cornell.cs.apl.viaduct.backend.aby.putNOTGate
import kotlin.random.Random

class ABYBench : CliktCommand(help = "Benchmark ABY operations") {
    private val role: Role by argument(help = "run as server or client").enum()
    private val count: Int by argument(help = "number of trials").int().default(1000)
    private val serverAddress by option(
        "-s", "--server",
        help = "set server address"
    )

    private val bitlen = 32.toLong()
    private val runningDurationMap: MutableMap<String, Double> = mutableMapOf()
    private val maxDurationMap: MutableMap<String, Double> = mutableMapOf()
    private val minDurationMap: MutableMap<String, Double> = mutableMapOf()

    private fun execCircuit(id: String, aby: ABYParty, buildCircuit: (ABYParty) -> Unit) {
        buildCircuit(aby)
        aby.execCircuit()
        val time: Double = aby.getTiming(Phase.P_TOTAL)
        aby.reset()

        runningDurationMap[id] = runningDurationMap[id]?.let { it + time } ?: + time
        minDurationMap[id] = minDurationMap[id]?.let { kotlin.math.min(it, time) } ?: time
        maxDurationMap[id] = maxDurationMap[id]?.let { kotlin.math.max(it, time) } ?: time
    }

    private fun binop(aby: ABYParty, type: SharingType, op: (Circuit, Share, Share) -> Share) {
        val circuit = aby.getCircuitBuilder(type)
        val lhs = if (role == Role.SERVER)
            circuit.putINGate(Random.nextInt().toBigInteger(), bitlen, role)
        else circuit.putDummyINGate(bitlen)

        val rhs = if (role == Role.SERVER)
            circuit.putDummyINGate(bitlen)
        else circuit.putINGate(Random.nextInt().toBigInteger(), bitlen, role)

        val opGate = op(circuit, lhs, rhs)
        circuit.putOUTGate(opGate, Role.ALL)
    }

    private fun add(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> circuit.putADDGate(lhs, rhs) }

    private fun sub(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> circuit.putSUBGate(lhs, rhs) }

    private fun mul(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> circuit.putMULGate(lhs, rhs) }

    private fun div(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> Aby.putInt32DIVGate(circuit, lhs, rhs) }

    private fun and(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> circuit.putANDGate(lhs, rhs) }

    private fun xor(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> circuit.putXORGate(lhs, rhs) }

    private fun eq(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> circuit.putEQGate(lhs, rhs) }

    private fun gt(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> circuit.putGTGate(lhs, rhs) }

    private fun min(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> Aby.putMinGate(circuit, lhs, rhs) }

    private fun max(aby: ABYParty, type: SharingType) =
        binop(aby, type) { circuit, lhs, rhs -> Aby.putMaxGate(circuit, lhs, rhs) }

    private fun inv(aby: ABYParty, type: SharingType) {
        val circuit = aby.getCircuitBuilder(type)
        val input = if (role == Role.SERVER)
            circuit.putINGate(Random.nextInt().toBigInteger(), bitlen, role)
        else circuit.putDummyINGate(bitlen)
        val inv = circuit.putNOTGate(input)
        circuit.putOUTGate(inv, Role.ALL)
    }

    private fun mux(aby: ABYParty, type: SharingType) {
        val circuit = aby.getCircuitBuilder(type)

        val guard = if (role == Role.SERVER)
            circuit.putINGate(Random.nextInt().toBigInteger(), bitlen, role)
        else circuit.putDummyINGate(bitlen)

        val lhs = if (role == Role.SERVER)
            circuit.putINGate(Random.nextInt().toBigInteger(), bitlen, role)
        else circuit.putDummyINGate(bitlen)

        val rhs = if (role == Role.SERVER)
            circuit.putINGate(Random.nextInt().toBigInteger(), bitlen, role)
        else circuit.putDummyINGate(bitlen)

        val mux = circuit.putMUXGate(lhs, rhs, guard)
        circuit.putOUTGate(mux, Role.ALL)
    }

    private fun conv(
        aby: ABYParty,
        inputType: SharingType,
        outputType: SharingType,
        convOp: (Circuit, Share) -> Share
    ) {
        val inputCircuit = aby.getCircuitBuilder(inputType)
        val input = if (role == Role.SERVER)
            inputCircuit.putINGate(Random.nextInt().toBigInteger(), bitlen, role)
        else inputCircuit.putDummyINGate(bitlen)

        val outputCircuit = aby.getCircuitBuilder(outputType)
        val convGate = convOp(outputCircuit, input)
        outputCircuit.putOUTGate(convGate, Role.ALL)
    }

    private fun convA2B(aby: ABYParty) =
        conv(aby, SharingType.S_ARITH, SharingType.S_BOOL) { circuit, input ->
            circuit.putA2BGate(input, aby.getCircuitBuilder(SharingType.S_YAO))
        }

    private fun convA2Y(aby: ABYParty) =
        conv(aby, SharingType.S_ARITH, SharingType.S_YAO) { circuit, input -> circuit.putA2YGate(input) }

    private fun convB2A(aby: ABYParty) =
        conv(aby, SharingType.S_BOOL, SharingType.S_ARITH) { circuit, input -> circuit.putB2AGate(input) }

    private fun convB2Y(aby: ABYParty) =
        conv(aby, SharingType.S_BOOL, SharingType.S_YAO) { circuit, input -> circuit.putB2YGate(input) }

    private fun convY2A(aby: ABYParty) =
        conv(aby, SharingType.S_YAO, SharingType.S_ARITH) { circuit, input ->
            circuit.putY2AGate(input, aby.getCircuitBuilder(SharingType.S_BOOL))
        }

    private fun convY2B(aby: ABYParty) =
        conv(aby, SharingType.S_YAO, SharingType.S_BOOL) { circuit, input -> circuit.putY2BGate(input) }

    override fun run() {
        val addrPort = (serverAddress ?: "127.0.0.1:5000").split(":", limit = 2)
        val address = addrPort[0]
        val port = addrPort[1].toInt()
        val aby = ABYParty(role, if (role == Role.SERVER) "" else address, port, Aby.getLT(), bitlen)

        for (i in 0 until count) {
            execCircuit("ARITH_ADD", aby) { add(aby, SharingType.S_ARITH) }
            execCircuit("BOOL_ADD", aby) { add(aby, SharingType.S_BOOL) }
            execCircuit("YAO_ADD", aby) { add(aby, SharingType.S_YAO) }

            execCircuit("ARITH_SUB", aby) { sub(aby, SharingType.S_ARITH) }
            execCircuit("BOOL_SUB", aby) { sub(aby, SharingType.S_BOOL) }
            execCircuit("YAO_SUB", aby) { sub(aby, SharingType.S_YAO) }

            execCircuit("ARITH_MUL", aby) { mul(aby, SharingType.S_ARITH) }
            execCircuit("BOOL_MUL", aby) { mul(aby, SharingType.S_BOOL) }
            execCircuit("YAO_MUL", aby) { mul(aby, SharingType.S_YAO) }

            execCircuit("ARITH_INV", aby) { inv(aby, SharingType.S_ARITH) }
            execCircuit("BOOL_INV", aby) { inv(aby, SharingType.S_BOOL) }
            execCircuit("YAO_INV", aby) { inv(aby, SharingType.S_YAO) }

            execCircuit("BOOL_DIV", aby) { div(aby, SharingType.S_BOOL) }
            execCircuit("YAO_DIV", aby) { div(aby, SharingType.S_YAO) }

            execCircuit("BOOL_AND", aby) { and(aby, SharingType.S_BOOL) }
            execCircuit("YAO_AND", aby) { and(aby, SharingType.S_YAO) }

            execCircuit("BOOL_XOR", aby) { xor(aby, SharingType.S_BOOL) }
            execCircuit("YAO_XOR", aby) { xor(aby, SharingType.S_YAO) }

            execCircuit("BOOL_EQ", aby) { eq(aby, SharingType.S_BOOL) }
            execCircuit("YAO_EQ", aby) { eq(aby, SharingType.S_YAO) }

            execCircuit("BOOL_GT", aby) { gt(aby, SharingType.S_BOOL) }
            execCircuit("YAO_GT", aby) { gt(aby, SharingType.S_YAO) }

            execCircuit("BOOL_MIN", aby) { min(aby, SharingType.S_BOOL) }
            execCircuit("YAO_MIN", aby) { min(aby, SharingType.S_YAO) }

            execCircuit("BOOL_MAX", aby) { max(aby, SharingType.S_BOOL) }
            execCircuit("YAO_MAX", aby) { max(aby, SharingType.S_YAO) }

            execCircuit("BOOL_MUX", aby) { mux(aby, SharingType.S_BOOL) }
            execCircuit("YAO_MUX", aby) { mux(aby, SharingType.S_YAO) }

            execCircuit("A2B", aby) { convA2B(aby) }
            execCircuit("A2Y", aby) { convA2Y(aby) }
            execCircuit("B2A", aby) { convB2A(aby) }
            execCircuit("B2Y", aby) { convB2Y(aby) }
            execCircuit("Y2A", aby) { convY2A(aby) }
            execCircuit("Y2B", aby) { convY2B(aby) }
        }

        for (kv in runningDurationMap) {
            val avg = kv.value / count.toDouble()
            val min = minDurationMap[kv.key]!!
            val max = maxDurationMap[kv.key]!!
            println("${kv.key}: $avg avg, $min min, $max max")
        }
    }
}
