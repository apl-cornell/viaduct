package edu.cornell.cs.apl.viaduct.backend.aby

import de.tu_darmstadt.cs.encrypto.aby.Circuit
import de.tu_darmstadt.cs.encrypto.aby.Role
import de.tu_darmstadt.cs.encrypto.aby.Share

class ABYCircuitBuilder(
    val circuit: Circuit,
    val bitlen: Long,
    val role: Role
)

sealed class ABYCircuitGate(val children: List<ABYCircuitGate>) {
    abstract fun buildABYGate(builder: ABYCircuitBuilder, children: List<Share>): Share
}

class ABYInGate(val value: Int) : ABYCircuitGate(listOf()) {
    override fun buildABYGate(builder: ABYCircuitBuilder, children: List<Share>): Share =
        builder.circuit.putINGate(value.toBigInteger(), builder.bitlen, builder.role)
}

class ABYDummyInGate : ABYCircuitGate(listOf()) {
    override fun buildABYGate(builder: ABYCircuitBuilder, children: List<Share>): Share =
        builder.circuit.putDummyINGate(builder.bitlen)
}

class ABYConstGate(val value: Int) : ABYCircuitGate(listOf()) {
    override fun buildABYGate(builder: ABYCircuitBuilder, children: List<Share>): Share =
        builder.circuit.putCONSGate(value.toBigInteger(), builder.bitlen)
}

class ABYOperationGate(
    val operation: ABYOperation,
    operands: List<ABYCircuitGate>
) : ABYCircuitGate(operands) {
    override fun buildABYGate(builder: ABYCircuitBuilder, children: List<Share>): Share =
        operation.buildABYGate(builder.circuit, children)
}

enum class ABYOperation {
    SUB_GATE {
        override val numOperands = 2
        override fun buildABYGate(circuit: Circuit, children: List<Share>): Share =
            circuit.putSUBGate(children[0], children[1])
    },
    ADD_GATE {
        override val numOperands = 2
        override fun buildABYGate(circuit: Circuit, children: List<Share>): Share =
            circuit.putADDGate(children[0], children[1])
    },
    MUL_GATE {
        override val numOperands = 2
        override fun buildABYGate(circuit: Circuit, children: List<Share>): Share =
            circuit.putMULGate(children[0], children[1])
    },
    AND_GATE {
        override val numOperands = 2
        override fun buildABYGate(circuit: Circuit, children: List<Share>): Share =
            circuit.putANDGate(children[0], children[1])
    },

    /*
    INV_GATE {
        override val numOperands = 1
        override fun buildABYGate(circuit: Circuit, children: List<Share>): Share =
            circuit.putINVGate(children[0].)
    },
    */
    GT_GATE {
        override val numOperands = 2
        override fun buildABYGate(circuit: Circuit, children: List<Share>): Share =
            circuit.putGTGate(children[0], children[1])
    },
    EQ_GATE {
        override val numOperands = 2
        override fun buildABYGate(circuit: Circuit, children: List<Share>): Share =
            circuit.putEQGate(children[0], children[1])
    },
    MUX_GATE {
        override val numOperands = 3
        override fun buildABYGate(circuit: Circuit, children: List<Share>): Share =
            circuit.putMUXGate(children[0], children[1], children[2])
    };

    abstract val numOperands: Int
    abstract fun buildABYGate(circuit: Circuit, children: List<Share>): Share
}
