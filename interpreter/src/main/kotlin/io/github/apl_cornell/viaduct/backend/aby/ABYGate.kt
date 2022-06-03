package io.github.apl_cornell.viaduct.backend.aby

import io.github.apl_cornell.aby.Aby
import io.github.apl_cornell.aby.Circuit
import io.github.apl_cornell.aby.Role
import io.github.apl_cornell.aby.Share
import io.github.apl_cornell.aby.UInt32Vector
import io.github.apl_cornell.viaduct.syntax.Operator
import io.github.apl_cornell.viaduct.syntax.operators.Addition
import io.github.apl_cornell.viaduct.syntax.operators.And
import io.github.apl_cornell.viaduct.syntax.operators.Division
import io.github.apl_cornell.viaduct.syntax.operators.EqualTo
import io.github.apl_cornell.viaduct.syntax.operators.ExclusiveOr
import io.github.apl_cornell.viaduct.syntax.operators.GreaterThan
import io.github.apl_cornell.viaduct.syntax.operators.GreaterThanOrEqualTo
import io.github.apl_cornell.viaduct.syntax.operators.LessThan
import io.github.apl_cornell.viaduct.syntax.operators.LessThanOrEqualTo
import io.github.apl_cornell.viaduct.syntax.operators.Maximum
import io.github.apl_cornell.viaduct.syntax.operators.Minimum
import io.github.apl_cornell.viaduct.syntax.operators.Multiplication
import io.github.apl_cornell.viaduct.syntax.operators.Mux
import io.github.apl_cornell.viaduct.syntax.operators.Negation
import io.github.apl_cornell.viaduct.syntax.operators.Not
import io.github.apl_cornell.viaduct.syntax.operators.Or
import io.github.apl_cornell.viaduct.syntax.operators.Subtraction

/**
 * A method that adds the gate for an operation with the given arguments to the circuit and returns
 * the resulting share.
 *
 * This is simply a generalization of the `putOPGate` methods (which can be unary, binary, or ternary)
 * to a list of arguments.
 */
typealias PutOperationGate = Circuit.(arguments: List<Share>) -> Share

enum class ABYCircuitType { ARITH, BOOL, YAO }

class ABYCircuitBuilder(
    val arithCircuit: Circuit,
    val boolCircuit: Circuit,
    val yaoCircuit: Circuit,
    val bitlen: Long,
    val role: Role
) {
    fun circuit(type: ABYCircuitType): Circuit =
        when (type) {
            ABYCircuitType.ARITH -> arithCircuit
            ABYCircuitType.BOOL -> boolCircuit
            ABYCircuitType.YAO -> yaoCircuit
        }
}

sealed class ABYCircuitGate(
    val children: List<ABYCircuitGate>,
    val circuitType: ABYCircuitType,
    var variableGate: Boolean = false // is this gate stored in a variable?
) {
    /** Adds the gate represented by this object to the given circuit. */
    abstract fun putGate(builder: ABYCircuitBuilder, childShares: List<Share>): Share
}

class ABYInGate(
    val value: Int,
    circuitType: ABYCircuitType
) : ABYCircuitGate(listOf(), circuitType) {
    override fun putGate(builder: ABYCircuitBuilder, childShares: List<Share>): Share =
        builder.circuit(circuitType).putINGate(value.toBigInteger(), builder.bitlen, builder.role)
}

class ABYDummyInGate(
    circuitType: ABYCircuitType
) : ABYCircuitGate(listOf(), circuitType) {
    override fun putGate(builder: ABYCircuitBuilder, childShares: List<Share>): Share =
        builder.circuit(circuitType).putDummyINGate(builder.bitlen)
}

class ABYConstantGate(
    val value: Int,
    circuitType: ABYCircuitType
) : ABYCircuitGate(listOf(), circuitType) {
    override fun putGate(builder: ABYCircuitBuilder, childShares: List<Share>): Share =
        builder.circuit(circuitType).putCONSGate(value.toBigInteger(), builder.bitlen)
}

class ABYConversionGate(
    inputGate: ABYCircuitGate,
    circuitType: ABYCircuitType
) : ABYCircuitGate(listOf(inputGate), circuitType) {
    override fun putGate(builder: ABYCircuitBuilder, childShares: List<Share>): Share =
        when (children[0].circuitType) {
            ABYCircuitType.ARITH ->
                when (circuitType) {
                    ABYCircuitType.ARITH ->
                        childShares[0]

                    ABYCircuitType.BOOL ->
                        builder.boolCircuit.putY2BGate(builder.yaoCircuit.putA2YGate(childShares[0]))

                    ABYCircuitType.YAO ->
                        builder.yaoCircuit.putA2YGate(childShares[0])
                }

            ABYCircuitType.BOOL ->
                when (circuitType) {
                    ABYCircuitType.ARITH ->
                        builder.arithCircuit.putB2AGate(childShares[0])

                    ABYCircuitType.BOOL ->
                        childShares[0]

                    ABYCircuitType.YAO ->
                        builder.yaoCircuit.putB2YGate(childShares[0])
                }

            ABYCircuitType.YAO ->
                when (circuitType) {
                    ABYCircuitType.ARITH ->
                        builder.arithCircuit.putB2AGate(builder.boolCircuit.putY2BGate(childShares[0]))

                    ABYCircuitType.BOOL ->
                        builder.boolCircuit.putY2BGate(childShares[0])

                    ABYCircuitType.YAO ->
                        childShares[0]
                }
        }
}

class ABYOperationGate(
    val operation: PutOperationGate,
    operands: List<ABYCircuitGate>,
    circuitType: ABYCircuitType
) : ABYCircuitGate(operands, circuitType) {
    override fun putGate(builder: ABYCircuitBuilder, childShares: List<Share>): Share =
        builder.circuit(circuitType).operation(childShares)
}

/** Add a conversion gate---if necessary---to match the target circuit type. */
fun ABYCircuitGate.addConversionGates(target: ABYCircuitType) =
    if (this.circuitType == target) this else ABYConversionGate(this, target)

/** Returns an ABY circuit implementing the given operator. */
fun operatorToCircuit(
    operator: Operator,
    arguments: List<ABYCircuitGate>,
    circuitType: ABYCircuitType
): ABYCircuitGate {
    val finalArguments = arguments.map { it.addConversionGates(circuitType) }
    return when {
        operator is Negation ->
            ABYOperationGate(
                putBinaryOperationGate(Circuit::putSUBGate),
                listOf(ABYConstantGate(0, circuitType), finalArguments[0]),
                circuitType
            )

        operator is Addition ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putADDGate), finalArguments, circuitType)

        operator is Subtraction ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putSUBGate), finalArguments.reversed(), circuitType)

        operator is Multiplication ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putMULGate), finalArguments, circuitType)

        operator is Minimum && circuitType != ABYCircuitType.ARITH ->
            ABYOperationGate(
                putBinaryOperationGate { lhs, rhs -> Aby.putMinGate(this, lhs, rhs) },
                listOf(finalArguments[0], finalArguments[1]),
                circuitType
            )

        operator is Maximum && circuitType != ABYCircuitType.ARITH ->
            ABYOperationGate(
                putBinaryOperationGate { lhs, rhs -> Aby.putMaxGate(this, lhs, rhs) },
                listOf(finalArguments[0], finalArguments[1]),
                circuitType
            )

        operator is Not && circuitType != ABYCircuitType.ARITH ->
            ABYOperationGate(putUnaryOperationGate(Circuit::putNOTGate), finalArguments, circuitType)

        operator is And && circuitType != ABYCircuitType.ARITH ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putANDGate), finalArguments, circuitType)

        operator is Or && circuitType != ABYCircuitType.ARITH ->
            // a | b = ~(~a & ~b)
            operatorToCircuit(
                Not,
                listOf(
                    operatorToCircuit(
                        And,
                        finalArguments.map { arg -> operatorToCircuit(Not, listOf(arg), circuitType) },
                        circuitType
                    )
                ),
                circuitType
            )

        operator is EqualTo && circuitType != ABYCircuitType.ARITH ->
            ABYOperationGate(putBinaryOperationGate(Circuit::putEQGate), finalArguments, circuitType)

        operator is LessThan && circuitType != ABYCircuitType.ARITH ->
            // x < y <=> y > x
            ABYOperationGate(
                putBinaryOperationGate(Circuit::putGTGate),
                listOf(finalArguments[0], finalArguments[1]),
                circuitType
            )

        operator is GreaterThan && circuitType != ABYCircuitType.ARITH ->
            // x < y <=> y > x
            ABYOperationGate(
                putBinaryOperationGate(Circuit::putGTGate),
                listOf(finalArguments[1], finalArguments[0]),
                circuitType
            )

        operator is LessThanOrEqualTo && circuitType != ABYCircuitType.ARITH ->
            // x <= y <=> not (x > y)
            operatorToCircuit(
                Not,
                listOf(
                    ABYOperationGate(
                        putBinaryOperationGate(Circuit::putGTGate),
                        finalArguments.reversed(),
                        circuitType
                    )
                ),
                circuitType
            )

        operator is GreaterThanOrEqualTo && circuitType != ABYCircuitType.ARITH ->
            // x >= y <=> (x > y)
            operatorToCircuit(
                Not,
                listOf(
                    ABYOperationGate(
                        putBinaryOperationGate(Circuit::putGTGate),
                        finalArguments,
                        circuitType
                    )
                ),
                circuitType
            )

        operator is Mux && circuitType != ABYCircuitType.ARITH ->
            ABYOperationGate(
                putTernaryOperationGate(Circuit::putMUXGate),
                listOf(finalArguments[0], finalArguments[2], finalArguments[1]),
                circuitType
            )

        operator is ExclusiveOr && circuitType != ABYCircuitType.ARITH ->
            ABYOperationGate(
                putBinaryOperationGate(Circuit::putXORGate),
                listOf(finalArguments[0], finalArguments[1]),
                circuitType
            )

        operator is Division && circuitType != ABYCircuitType.ARITH ->
            ABYOperationGate(
                putBinaryOperationGate { lhs, rhs -> Aby.putInt32DIVGate(this, lhs, rhs) },
                listOf(finalArguments[0], finalArguments[1]),
                circuitType
            )

        else -> throw UnsupportedOperationException("Operator $operator in sharing $circuitType is not supported by the ABY backend.")
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
fun Circuit.putNOTGate(input: Share): Share {
    val inverses = mutableListOf<Long>()
    for (wire in input.wires) {
        inverses.add(this.putINVGate(wire))
    }
    return Aby.createNewShare(UInt32Vector(inverses), this)
}
