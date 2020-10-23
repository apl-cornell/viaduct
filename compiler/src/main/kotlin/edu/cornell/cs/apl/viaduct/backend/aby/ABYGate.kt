package edu.cornell.cs.apl.viaduct.backend.aby

import de.tu_darmstadt.cs.encrypto.aby.Aby
import de.tu_darmstadt.cs.encrypto.aby.Circuit
import de.tu_darmstadt.cs.encrypto.aby.Role
import de.tu_darmstadt.cs.encrypto.aby.Share
import de.tu_darmstadt.cs.encrypto.aby.UInt32Vector
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThan
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThanOrEqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Negation
import edu.cornell.cs.apl.viaduct.syntax.operators.Not
import edu.cornell.cs.apl.viaduct.syntax.operators.Or
import edu.cornell.cs.apl.viaduct.syntax.operators.Subtraction

/**
 * A method that adds the gate for an operation with the given arguments to the circuit and returns
 * the resulting share.
 *
 * This is simply a generalization of the `putOPGate` methods (which can be unary, binary, or ternary)
 * to a list of arguments.
 */
typealias PutOperationGate = Circuit.(arguments: List<Share>) -> Share

class ABYCircuitBuilder(
    val circuit: Circuit,
    val bitlen: Long,
    val role: Role
)

sealed class ABYCircuitGate(val children: List<ABYCircuitGate>) {
    /** Adds the gate represented by this object to the given circuit. */
    abstract fun putGate(builder: ABYCircuitBuilder, children: List<Share>): Share
}

class ABYInGate(val value: Int) : ABYCircuitGate(listOf()) {
    override fun putGate(builder: ABYCircuitBuilder, children: List<Share>): Share =
        builder.circuit.putINGate(value.toBigInteger(), builder.bitlen, builder.role)
}

class ABYDummyInGate : ABYCircuitGate(listOf()) {
    override fun putGate(builder: ABYCircuitBuilder, children: List<Share>): Share =
        builder.circuit.putDummyINGate(builder.bitlen)
}

class ABYConstantGate(val value: Int) : ABYCircuitGate(listOf()) {
    override fun putGate(builder: ABYCircuitBuilder, children: List<Share>): Share =
        builder.circuit.putCONSGate(value.toBigInteger(), builder.bitlen)
}

class ABYOperationGate(
    val operation: PutOperationGate,
    operands: List<ABYCircuitGate>
) : ABYCircuitGate(operands) {
    override fun putGate(builder: ABYCircuitBuilder, children: List<Share>): Share =
        builder.circuit.operation(children)
}

/** Returns an ABY circuit implementing the given operator. */
fun operatorToCircuit(operator: Operator, arguments: List<ABYCircuitGate>): ABYCircuitGate {
    return when (operator) {
        is Negation ->
            ABYOperationGate(
                putBinaryOperationGate(Circuit::putSUBGate),
                listOf(ABYConstantGate(0), arguments[0])
            )

        is Addition ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putADDGate), arguments)

        is Subtraction ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putSUBGate), arguments.reversed())

        is Multiplication ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putMULGate), arguments)

        is Minimum ->
            operatorToCircuit(
                Mux,
                listOf(
                    operatorToCircuit(LessThan, arguments),
                    arguments[0],
                    arguments[1]
                )
            )

        is Maximum ->
            operatorToCircuit(
                Mux,
                listOf(
                    operatorToCircuit(LessThan, arguments),
                    arguments[1],
                    arguments[0]
                )
            )

        is Not ->
            ABYOperationGate(putUnaryOperationGate(Circuit::putNOTGate), arguments)

        is And ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putANDGate), arguments)

        is Or ->
            // a | b = ~(~a & ~b)
            operatorToCircuit(
                Not,
                listOf(operatorToCircuit(And, arguments.map { arg -> operatorToCircuit(Not, listOf(arg)) }))
            )

        is EqualTo ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putEQGate), arguments)

        is LessThan ->
            // x < y <=> y > x
            ABYOperationGate(
                putBinaryOperationGate(Circuit::putGTGate),
                listOf(arguments[0], arguments[1])
            )

        is LessThanOrEqualTo ->
            // x <= y <=> not (x > y)
            operatorToCircuit(
                Not,
                listOf(ABYOperationGate(putBinaryOperationGate(Circuit::putGTGate), arguments.reversed()))
            )

        is Mux ->
            ABYOperationGate(
                putTernaryOperationGate(Circuit::putMUXGate),
                listOf(arguments[0], arguments[2], arguments[1])
            )

        else -> throw UnsupportedOperationException("Operator $operator is not supported by the ABY backend.")
    }
}

/** Wraps the put method of a unary gate as a generic [PutOperationGate]. */
private fun putUnaryOperationGate(gate: Circuit.(Share) -> Share): PutOperationGate = { arguments ->
    this.gate(arguments[0])
}

/** Wraps the put method of a binary gate as a generic [PutOperationGate]. */
private fun putBinaryOperationGate(gate: Circuit.(Share, Share) -> Share): PutOperationGate = { arguments ->
    this.gate(arguments[0], arguments[1])
}

/** Wraps the put method of a ternary gate as a generic [PutOperationGate]. */
private fun putTernaryOperationGate(gate: Circuit.(Share, Share, Share) -> Share): PutOperationGate = { arguments ->
    this.gate(arguments[0], arguments[1], arguments[2])
}

/** Implements bitwise not. */
private fun Circuit.putNOTGate(input: Share): Share {
    val inverses = mutableListOf<Long>()
    for (wire in input.wires) {
        inverses.add(this.putINVGate(wire))
    }
    return Aby.createNewShare(UInt32Vector(inverses), this)
}
