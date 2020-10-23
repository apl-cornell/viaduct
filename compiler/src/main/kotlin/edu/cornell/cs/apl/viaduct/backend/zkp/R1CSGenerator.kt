package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.backend.WireConst
import edu.cornell.cs.apl.viaduct.backend.WireDummyIn
import edu.cornell.cs.apl.viaduct.backend.WireIn
import edu.cornell.cs.apl.viaduct.backend.WireOp
import edu.cornell.cs.apl.viaduct.backend.WireTerm
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.Constraint
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.LinComb
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.R1CS
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.Term
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.TermVector
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.WireInfo
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.WireType
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo

typealias Wire = Int

data class LinTerm(val constTerm: Int, val linearTerm: List<Pair<Int, Wire>>) {
    companion object {
        fun fromWire(w: Wire): LinTerm {
            return LinTerm(0, listOf(1 to w))
        }

        fun fromConst(v: Int): LinTerm {
            return LinTerm(v, listOf())
        }
    }

    fun toJNI(): edu.cornell.cs.apl.viaduct.libsnarkwrapper.LinComb {
        val lc = LinComb()
        lc.constTerm = constTerm
        lc.linTerms = TermVector(linearTerm.map {
            val tm = Term()
            tm.coeff = it.first
            tm.wireID = it.second
            tm
        })
        return lc
    }

    fun add(other: LinTerm): LinTerm {
        return LinTerm(constTerm + other.constTerm, linearTerm + other.linearTerm)
    }
}

data class ConstraintTuple(val lhs : LinTerm, val rhs : LinTerm, val eq : LinTerm)

class R1CSGenerator() {
    init {
        System.loadLibrary("snarkwrapper")
    }

    private val r1cs: R1CS = R1CS()

    fun mkInput(v: Int): Wire {
        val wi = WireInfo()
        wi.type = WireType.WIRE_IN
        wi.input_val = v
        return r1cs.mkWire(wi)
    }

    fun mkDummy(): Wire {
        val wi = WireInfo()
        wi.type = WireType.WIRE_DUMMY_IN
        return r1cs.mkWire(wi)
    }

    fun mkPublicInput(v: Int): Wire {
        val wi = WireInfo()
        wi.type = WireType.WIRE_PUBLIC_IN
        wi.input_val = v
        return r1cs.mkWire(wi)
    }

    fun mkInternal(): Wire {
        val wi = WireInfo()
        wi.type = WireType.WIRE_INTERNAL
        return r1cs.mkWire(wi)
    }

    fun addConstraint(ctup : ConstraintTuple) {
        val c = Constraint()
        c.lhs = ctup.lhs.toJNI()
        c.rhs = ctup.rhs.toJNI()
        c.eq = ctup.eq.toJNI()
        r1cs.addConstraint(c)
    }

    fun makeProof(pk: String): String {
        return r1cs.generateProof(pk)
    }

    fun verifyProof(vk: String, pf: String): Int {
        return r1cs.verifyProof(vk, pf)
    }
}

fun R1CSGenerator.assertEqualsTo(w: Wire, v: Int) {
    addConstraint(ConstraintTuple(LinTerm.fromWire(w), LinTerm.fromConst(1), LinTerm.fromConst(v)))
}

fun WireTerm.getWire(generator: R1CSGenerator): Wire {
    return when (this) {
        is WireOp -> generator.mkWireOp(this.op, this.inputs.map { it.getWire(generator) })
        is WireIn -> generator.mkInput(this.v)
        is WireDummyIn -> generator.mkDummy()
        is WireConst -> generator.mkPublicInput(this.v)
    }
}


fun Operator.interpretR1CS() : ((List<Wire>, Wire) -> ConstraintTuple)? {
    return when(this) {
        is Addition -> { args, i ->
            ConstraintTuple(
                LinTerm.fromConst(1).add(LinTerm.fromWire(args[0]).add(LinTerm.fromWire(args[1]))),
                LinTerm.fromConst(1),
                LinTerm.fromWire(i)
            )
        }
        is EqualTo -> { args, i ->
            ConstraintTuple(
                LinTerm(0, listOf(1 to args[0], -1 to args[1])),
                LinTerm.fromConst(1),
                LinTerm.fromWire(i)
            )
        }
        is And -> { args, i ->
           ConstraintTuple(
               LinTerm(0, listOf(1 to args[0])),
               LinTerm(0, listOf(1 to args[1])),
               LinTerm.fromWire(i)
           )
        }
        else -> null
        }
}

fun R1CSGenerator.mkWireOp(op: Operator, args: List<Wire>): Wire {
    val k = op.interpretR1CS()
    if (k == null) {
        throw Exception("unknown op $op")
    } else {
        val i = this.mkInternal()
        this.addConstraint(k(args, i))
        return i
    }
}

fun WireTerm.toR1CS(is_eq_to: Int): R1CSGenerator {
    val gen = R1CSGenerator()
    val o = this.getWire(gen)
    gen.assertEqualsTo(o, is_eq_to)
    return gen
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

