package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.backend.WireConst
import edu.cornell.cs.apl.viaduct.backend.WireDummyIn
import edu.cornell.cs.apl.viaduct.backend.WireIn
import edu.cornell.cs.apl.viaduct.backend.WireOp
import edu.cornell.cs.apl.viaduct.backend.WireTerm
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition

import edu.cornell.cs.apl.viaduct.backend.zkp.LibsnarkJNI

typealias Wire = Int

typealias FieldVal = Int

sealed class WireType
object InputWire : WireType()
object AuxInputWire : WireType()
object InternalWire : WireType()

data class LinComb(val constTerm: FieldVal, val linearTerm: List<Pair<FieldVal, Wire>>)
data class R1CSConstraint(val lhs: LinComb, val rhs: LinComb, val eq: LinComb)
data class R1CS(val wires: List<WireType>, val constraints: Set<R1CSConstraint>)

class R1CSGenerator {
    private val wires: MutableList<WireType> = mutableListOf()
    private val constraints: MutableSet<R1CSConstraint> = mutableSetOf()

    fun mkInput(): Wire {
        wires.add(InputWire)
        return wires.size - 1
    }

    fun mkAuxInput(): Wire {
        wires.add(AuxInputWire)
        return wires.size - 1
    }

    fun mkInternal(): Wire {
        wires.add(InternalWire)
        return wires.size - 1
    }

    fun addConstraint(lhs: LinComb, rhs: LinComb, eq: LinComb) {
        constraints.add(R1CSConstraint(lhs, rhs, eq))
    }

    fun getR1CS(): R1CS {
        return R1CS(wires, constraints)
    }
}

fun R1CSGenerator.assertEqualsTo(w: Wire, v: FieldVal) {
    addConstraint(LinComb(0, listOf(1 to w)), LinComb(1, listOf()), LinComb(v, listOf()))
}

fun WireTerm.getWire(generator: R1CSGenerator): Wire {
    return when (this) {
        is WireOp -> generator.mkWireOp(this.op, this.inputs.map { it.getWire(generator) })
        is WireIn -> generator.mkInput()
        is WireDummyIn -> generator.mkInput()
        is WireConst -> generator.mkAuxInput()
    }
}

fun R1CSGenerator.mkWireOp(op: Operator, args: List<Wire>): Wire {
    return when (op) {
        is Addition -> {
            val i = this.mkInternal()
            this.addConstraint(
                LinComb(0, listOf(1 to args[0], 1 to args[1])),
                LinComb(1, listOf()),
                LinComb(0, listOf(1 to i))
            )
            i
        }
        else -> throw Exception("Unknown op")
    }
}

fun WireTerm.toR1CS(is_eq_to: Int): R1CS {
    val gen = R1CSGenerator()
    val o = this.getWire(gen)
    gen.assertEqualsTo(o, is_eq_to)
    return gen.getR1CS()
}

fun WireTerm.secretInputs(): Map<Int, Int> {
    val ret: MutableMap<Int, Int> = mutableMapOf()

    fun WireTerm.traverse() {
        when (this) {
            is WireOp -> {
                inputs.map {
                    it.traverse()
                }
            }
            is WireIn -> ret[this.index] = this.v
            is WireDummyIn -> {
            }
            is WireConst -> {
            }
        }
    }
    this.traverse()
    return ret
}

fun WireTerm.auxInputs(): Map<Int, Int> {
    val ret: MutableMap<Int, Int> = mutableMapOf()

    fun WireTerm.traverse() {
        when (this) {
            is WireOp -> {
                inputs.map {
                    it.traverse()
                }
            }
            is WireIn -> {
            }
            is WireDummyIn -> {
            }
            is WireConst -> ret[this.index] = this.v
        }
    }
    this.traverse()
    return ret
}


