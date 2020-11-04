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
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Not
import edu.cornell.cs.apl.viaduct.syntax.operators.Or
import mu.KotlinLogging
import kotlin.math.pow

// Needs to be in sync with getOutputWire below
fun Operator.supportedOp(): Boolean =
    when (this) {
        is And, Not, Or, Multiplication, Addition, EqualTo, Mux -> true
        else -> false
    }

// For boolean values, we have as an invariant that they are \in {0, 1}
fun Operator.getOutputWire(instance: R1CSInstance, args: List<Wire>): Wire {
    return when (this) {
        is Not -> { // (1-x) * 1
            val lhs = LinTerm(1, listOf(-1L to args[0]))
            val rhs = LinTerm(1, listOf())
            val w = instance.mkInternal(lhs, rhs)
            instance.assertBoolean(w)
            w
        }
        is And -> { // x * y
            val lhs = LinTerm(0, listOf(1L to args[0]))
            val rhs = LinTerm(0, listOf(1L to args[1]))
            val w = instance.mkInternal(lhs, rhs)
            instance.assertBoolean(w)
            w
        }
        is Multiplication -> {
            val lhs = LinTerm(0, listOf(1L to args[0]))
            val rhs = LinTerm(0, listOf(1L to args[1]))
            val w = instance.mkInternal(lhs, rhs)
            w
        }
        is Or -> { // x | y = ! (!x && !y)
            Not.getOutputWire(
                instance, listOf(
                    And.getOutputWire(
                        instance, listOf(
                            Not.getOutputWire(instance, listOf(args[0])),
                            Not.getOutputWire(instance, listOf(args[1]))
                        )
                    )
                )
            )
        }
        is Mux -> { // b ? x : y = b * x + (1-b) * y
            val b = LinTerm(0, listOf(1L to args[0]))
            val negb = LinTerm(1, listOf(-1L to args[0]))
            val x = LinTerm(0, listOf(1L to args[1]))
            val y = LinTerm(0, listOf(1L to args[2]))
            val ifTrue = instance.mkInternal(b, x)
            val ifFalse = instance.mkInternal(negb, y)
            val res = instance.mkInternal(
                LinTerm(0, listOf(1L to ifTrue, 1L to ifFalse)),
                LinTerm.fromConst(1)
            )
            res
        }
        is Addition -> {
            val lhs = LinTerm(0, listOf(1L to args[0], 1L to args[1]))
            return instance.mkInternal(lhs, LinTerm.fromConst(1))
        }
        is EqualTo -> {
            val ws1 = instance.mkUnpack(args[0])
            val ws2 = instance.mkUnpack(args[1])
            fun mkIff(a: Wire, b: Wire): Wire { // a = b := (ab) + !(a+b)
                val ab = And.getOutputWire(instance, listOf(a, b))
                val aorb = Or.getOutputWire(instance, listOf(a, b))
                val not_aorb = Not.getOutputWire(instance, listOf(aorb))
                return Or.getOutputWire(instance, listOf(ab, not_aorb))
            }
            return ws1.zip(ws2).map { mkIff(it.first, it.second) }.reduce { a, b ->
                And.getOutputWire(instance, listOf(a, b))
            }
        }

        else -> throw java.lang.Exception("Unsupported op: $this")
    }
}

typealias Wire = Int

data class LinTerm(val constTerm: Long, val linearTerm: List<Pair<Long, Wire>>) {
    companion object {
        fun fromWire(w: Wire): LinTerm {
            return LinTerm(0, listOf(1L to w))
        }

        fun fromConst(v: Long): LinTerm {
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

    private val wireValues: MutableMap<Wire, Long> = mutableMapOf()

    fun getWireVal(w: Wire): Long {
        return wireValues[w] ?: throw Exception("Unknown value for wire $w")
    }

    fun setWireVal(w: Wire, l: Long) {
        assert(!wireValues.containsKey(w))
        wireValues[w] = l
    }

    fun addConstraint(ctup: ConstraintTuple, name: String = "") {
        val c = Constraint()
        c.lhs = ctup.lhs.toJNI()
        c.rhs = ctup.rhs.toJNI()
        c.eq = ctup.eq.toJNI()
        c.annotation = name
        r1cs.addConstraint(c)
    }

    fun mkInput(index: Int, v: Long): Wire {
        assert(isProver)
        return if (secretInputs.containsKey(index)) {
            secretInputs[index]!!
        } else {
            val wi = WireInfo()
            wi.type = WireType.WIRE_IN
            wi.input_val = v
            val w = r1cs.mkWire(wi, "In($index, $v)")
            secretInputs[index] = w
            setWireVal(w, v)
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
            val w = r1cs.mkWire(wi, "In($index)")
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
            wi.input_val = v.toLong()
            val w = r1cs.mkWire(wi, "Public($index, $v)")
            logger.info { "Made public input with wire $w and value $v " }
            publicInputs[index] = w
            setWireVal(w, v.toLong())
            w
        }
    }

    fun mkOutput(v: Int): Wire {
        return if (outputWire != null) {
            outputWire!!
        } else {
            val wi = WireInfo()
            wi.type = WireType.WIRE_PUBLIC_IN
            wi.input_val = v.toLong()
            val w = r1cs.mkWire(wi, "Output($v)")
            outputWire = w
            setWireVal(w, v.toLong())
            w
        }
    }

    fun evaluate(lt: LinTerm): Long {
        var res = 0L
        res += lt.constTerm
        for (i in lt.linearTerm) {
            res += i.first * getWireVal(i.second)
        }
        return res
    }


    fun mkInternalProver(v : Long, name : String = "") : Wire {
        assert(isProver)
        val wi = WireInfo()
        wi.type = WireType.WIRE_IN
        wi.input_val = v
        val w = r1cs.mkWire(wi, name)
        setWireVal(w, v)
        return w
    }

    fun mkInternalVerifier(name : String = "") : Wire {
        assert(!isProver)
        val wi = WireInfo()
        wi.type = WireType.WIRE_DUMMY_IN
        val w = r1cs.mkWire(wi, name)
        return w
    }

    fun mkInternal(lhs: LinTerm, rhs: LinTerm): Wire {
        if (isProver) {
            val w = mkInternalProver(evaluate(lhs) * evaluate(rhs), "Internal")
            addConstraint(ConstraintTuple(lhs, rhs, LinTerm.fromWire(w)))
            return w
        } else {
            val w = mkInternalVerifier("Internal")
            addConstraint(ConstraintTuple(lhs, rhs, LinTerm.fromWire(w)))
            return w
        }
    }

    private fun genBooleanProver(b: Byte): Wire {
        val w = mkInternalProver(b.toLong(), "InternalBool($b)")
        assertBoolean(w)
        return w
    }

    private fun genBooleanVerifer(): Wire =
        mkInternalVerifier("InternalBool")

    private fun getBit(l: Long, pos: Int): Byte {
        return (l.shr(pos)).and(1).toByte()
    }

    val powersOf232: List<Long> =
        (0..32).map {
            (2).toDouble().pow(it).toLong()
        }

    // TODO support two's complement
    fun mkUnpack(w: Wire): List<Wire> {
        if (isProver) {
            val v = getWireVal(w)
            assert(v >= 0)
            val ws = (0..32).map { genBooleanProver(getBit(v, it)) }
            val tms = ws.zip(powersOf232).map {
                it.second to it.first
            }
            logger.info { " Unpacking wire $w = $v with values ${ws.map { getWireVal(it) }}" }
            logger.info { " Repacked: ${evaluate(LinTerm(0, tms))}" }
            addConstraint(
                ConstraintTuple(
                    LinTerm.fromConst(1),
                    LinTerm(0, tms),
                    LinTerm.fromWire(w)
                ), "unpack32: $w"
            )
            return ws
        } else {
            val ws = (0..32).map { genBooleanVerifer() }
            val tms = ws.zip(powersOf232).map {
                it.second to it.first
            }
            addConstraint(
                ConstraintTuple(
                    LinTerm.fromConst(1),
                    LinTerm(0, tms),
                    LinTerm.fromWire(w)
                )
            )
            return ws
        }
    }

    // x * (1- x) = 0
    fun assertBoolean(w: Wire) {
        addConstraint(ConstraintTuple(LinTerm.fromWire(w), LinTerm(1, listOf(-1L to w)), LinTerm.fromConst(0)))
    }

    fun assertEquality(w1: Wire, w2: Wire) {
        addConstraint(ConstraintTuple(LinTerm.fromWire(w1), LinTerm.fromConst(1), LinTerm.fromWire(w2)))
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
        logger.info { "Wire values: $wireValues" }
        return r1cs.genKeypair()
    }
}

fun WireTerm.generatePrimaryInputs(instance: R1CSInstance) {
    when (this) {
        is WireConst -> {
            // TODO: lift restriction by unpacking into two's complement
            assert(this.v >= 0)
            instance.mkPublicInput(this.index, this.v)
        }
        is WireOp -> this.inputs.map { it.generatePrimaryInputs(instance) }
        is WireIn -> {
        }
        is WireDummyIn -> {
        }
    }
}

fun WireTerm.getWire(isProver: Boolean, instance: R1CSInstance): Wire {
    return when (this) {
        is WireOp -> this.op.getOutputWire(instance, this.inputs.map { it.getWire(isProver, instance) })
        is WireIn -> {
            assert(isProver)
            // TODO: lift restriction by unpacking into two's complement
            assert(this.v >= 0)
            instance.mkInput(this.index, this.v.toLong())
        }
        is WireDummyIn -> {
            assert(!isProver)
            instance.mkDummy(this.index)
        }
        // At this point the wire is already cached
        is WireConst -> instance.mkPublicInput(this.index, this.v)
    }
}

private val logger = KotlinLogging.logger("ZKP Generator")

fun WireTerm.toR1CS(isProver: Boolean, is_eq_to: Int): R1CSInstance {
    val instance = R1CSInstance(isProver)
    val publicOutput = instance.mkOutput(is_eq_to)
    // First make sure all the primary inputs are instantiated, because they come first
    this.generatePrimaryInputs(instance)
    // Now, go through the circuit (generating aux inputs along the way)
    val wireOutput = this.getWire(isProver, instance)
    logger.info { "Making r1cs instance with wireval $is_eq_to" }
    instance.assertEquality(wireOutput, publicOutput)
    return instance
}
