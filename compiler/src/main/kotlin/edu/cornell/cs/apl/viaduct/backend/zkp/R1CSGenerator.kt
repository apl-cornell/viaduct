package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.backend.WireConst
import edu.cornell.cs.apl.viaduct.backend.WireDummyIn
import edu.cornell.cs.apl.viaduct.backend.WireIn
import edu.cornell.cs.apl.viaduct.backend.WireOp
import edu.cornell.cs.apl.viaduct.backend.WireTerm
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.ByteBuf
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.Constraint
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.Keypair
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.LinComb
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.R1CS
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.Term
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.TermVector
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.WireInfo
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.WireType
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.libsnarkwrapper
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.Not
import edu.cornell.cs.apl.viaduct.syntax.operators.Or

// Needs to be in sync with getOutputWire below
fun Operator.supportedOp(): Boolean =
    when (this) {
        is And, Not, Or -> true
        else -> false
    }

// x | y = !(!x & !y) = 1 - ((1-x) * (1-y)) = 1 - (1 - x - y + xy) = x + y - xy
// For boolean values, we have as an invariant that they are \in {0, 1}

// b ? x : y = b*x + (1-b) * y

fun Operator.getOutputWire(isProver: Boolean, instance: R1CSInstance, args: List<Wire>): Wire {
    return when (this) {
        is Not -> {
            val lhs = LinTerm(1, listOf(-1 to args[0]))
            val rhs = LinTerm(1, listOf())
            if (isProver) { // if we are the prover..
                val v = (1 - instance.getWireVal(args[0]))
                val w = instance.mkInternalProver(lhs, rhs, v)
                instance.assertBoolean(w)
                w
            } else {
                val w = instance.mkInternalVerifier(lhs, rhs)
                instance.assertBoolean(w)
                w
            }
        }
        is And -> {
            val lhs = LinTerm(0, listOf(1 to args[0]))
            val rhs = LinTerm(0, listOf(1 to args[1]))
            if (isProver) {
                val v = instance.getWireVal(args[0]) * instance.getWireVal(args[1])
                val w = instance.mkInternalProver(lhs, rhs, v)
                instance.assertBoolean(w)
                w
            } else {
                val w = instance.mkInternalVerifier(lhs, rhs)
                instance.assertBoolean(w)
                w
            }
        }
        else -> throw java.lang.Exception("Unsupported op: $this")
    }
}

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

    fun toJNI(): LinComb {
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

data class ConstraintTuple(val lhs: LinTerm, val rhs: LinTerm, val eq: LinTerm)

class R1CSInstance(val isProver: Boolean) {
    init {
        System.loadLibrary("snarkwrapper")
        libsnarkwrapper.initZKP()
    }

    private val r1cs: R1CS = R1CS()
    private val publicInputs: MutableMap<Int, Wire> = mutableMapOf()
    private val secretInputs: MutableMap<Int, Wire> = mutableMapOf()
    private var outputWire: Wire? = null

    val wireValues: MutableMap<Wire, Int> = mutableMapOf()

    fun getWireVal(w: Wire): Int {
        return wireValues[w] ?: throw Exception("Unknown value for wire $w")
    }

    fun addConstraint(ctup: ConstraintTuple) {
        val c = Constraint()
        c.lhs = ctup.lhs.toJNI()
        c.rhs = ctup.rhs.toJNI()
        c.eq = ctup.eq.toJNI()
        r1cs.addConstraint(c)
    }

    fun mkInput(index: Int, v: Int): Wire {
        assert(isProver)
        return if (secretInputs.containsKey(index)) {
            secretInputs[index]!!
        } else {
            val wi = WireInfo()
            wi.type = WireType.WIRE_IN
            wi.input_val = v
            val w = r1cs.mkWire(wi)
            secretInputs[index] = w
            wireValues[w] = v
            w
        }
    }

    fun mkDummy(index: Int): Wire {
        assert(!isProver)
        return if (secretInputs.containsKey(index)) {
            secretInputs[index]!!
        } else {
            val wi = WireInfo()
            wi.type = WireType.WIRE_DUMMY_IN
            val w = r1cs.mkWire(wi)
            secretInputs[index] = w
            w
        }
    }

    fun mkPublicInput(index: Int, v: Int): Wire {
        return if (publicInputs.containsKey(index)) {
            publicInputs[index]!!
        } else {
            val wi = WireInfo()
            wi.type = WireType.WIRE_PUBLIC_IN
            wi.input_val = v
            val w = r1cs.mkWire(wi)
            wireValues[w] = v
            w
        }
    }

    fun mkOutput(v: Int): Wire {
        return if (outputWire != null) {
            outputWire!!
        } else {
            val wi = WireInfo()
            wi.type = WireType.WIRE_PUBLIC_IN
            wi.input_val = v
            val w = r1cs.mkWire(wi)
            outputWire = w
            w
        }
    }

    fun mkInternalProver(lhs: LinTerm, rhs: LinTerm, v: Int): Wire {
        val wi = WireInfo()
        wi.type = WireType.WIRE_IN
        wi.input_val = v
        val w = r1cs.mkWire(wi)
        addConstraint(ConstraintTuple(lhs, rhs, LinTerm.fromWire(w)))
        wireValues[w] = v
        return w
    }

    fun mkInternalVerifier(lhs: LinTerm, rhs: LinTerm): Wire {
        val wi = WireInfo()
        wi.type = WireType.WIRE_DUMMY_IN
        val w = r1cs.mkWire(wi)
        addConstraint(ConstraintTuple(lhs, rhs, LinTerm.fromWire(w)))
        return w
    }

    fun makeProof(pk: ByteBuf): ByteBuf {
        assert(outputWire != null)
        return r1cs.generateProof(pk)
    }

    fun verifyProof(vk: ByteBuf, pf: ByteBuf): Boolean {
        assert(outputWire != null)
        return r1cs.verifyProof(vk, pf)
    }

    fun genKeypair(): Keypair {
        assert(outputWire != null)
        return r1cs.genKeypair()
    }
}

fun R1CSInstance.assertEqualsTo(w: Wire, v: Wire) {
    addConstraint(ConstraintTuple(LinTerm.fromWire(w), LinTerm.fromConst(1), LinTerm.fromWire(v)))
}

// x * (1- x) = 0
fun R1CSInstance.assertBoolean(w: Wire) {
    addConstraint(ConstraintTuple(LinTerm.fromWire(w), LinTerm(1, listOf(-1 to w)), LinTerm.fromConst(0)))
}

fun WireTerm.generatePrimaryInputs(instance : R1CSInstance) {
    when (this) {
        is WireConst -> instance.mkPublicInput(this.index, this.v)
        is WireOp -> this.inputs.map { it.generatePrimaryInputs(instance) }
        is WireIn ->  { }
        is WireDummyIn -> { }
    }
}

fun WireTerm.getWire(isProver: Boolean, instance: R1CSInstance): Wire {
    return when (this) {
        is WireOp -> this.op.getOutputWire(isProver, instance, this.inputs.map { it.getWire(isProver, instance) })
        is WireIn -> {
            assert(isProver)
            instance.mkInput(this.index, this.v)
        }
        is WireDummyIn -> {
            assert(!isProver)
            instance.mkDummy(this.index)
        }
        is WireConst -> instance.mkPublicInput(this.index, this.v)
    }
}


fun WireTerm.toR1CS(isProver: Boolean, is_eq_to: Int): R1CSInstance {
    val instance = R1CSInstance(isProver)
    val w_o = instance.mkOutput(is_eq_to)
    // First make sure all the primary inputs are instantiated, because they come first
    this.generatePrimaryInputs(instance)
    // Now, go through the circuit (generating aux inputs along the way)
    val o = this.getWire(isProver, instance)
    instance.assertEqualsTo(o, w_o)
    return instance
}

