package io.github.aplcornell.viaduct.backend.zkp

import io.github.aplcornell.viaduct.backend.WireConst
import io.github.aplcornell.viaduct.backend.WireDummyIn
import io.github.aplcornell.viaduct.backend.WireIn
import io.github.aplcornell.viaduct.backend.WireOp
import io.github.aplcornell.viaduct.backend.WireTerm
import io.github.aplcornell.viaduct.libsnarkwrapper.ByteBuf
import io.github.aplcornell.viaduct.libsnarkwrapper.Keypair
import io.github.aplcornell.viaduct.libsnarkwrapper.R1CSInstance
import io.github.aplcornell.viaduct.libsnarkwrapper.Var
import io.github.aplcornell.viaduct.libsnarkwrapper.VarArray
import io.github.aplcornell.viaduct.libsnarkwrapper.libsnarkwrapper
import io.github.aplcornell.viaduct.libsnarkwrapper.libsnarkwrapper.mkByteBuf
import io.github.aplcornell.viaduct.syntax.operators.Addition
import io.github.aplcornell.viaduct.syntax.operators.And
import io.github.aplcornell.viaduct.syntax.operators.EqualTo
import io.github.aplcornell.viaduct.syntax.operators.LessThan
import io.github.aplcornell.viaduct.syntax.operators.LessThanOrEqualTo
import io.github.aplcornell.viaduct.syntax.operators.Multiplication
import io.github.aplcornell.viaduct.syntax.operators.Mux
import io.github.aplcornell.viaduct.syntax.operators.Not
import io.github.aplcornell.viaduct.syntax.operators.Or
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ZKP Generator")

class ZKPInit {
    companion object {
        init {
            System.loadLibrary("snarkwrapper")
            libsnarkwrapper.initZKP()
        }
    }
}

// Assumes libsnarkwrapper has been initialized, and initZKP has been called already
class R1CS(val isProver: Boolean, val wire: WireTerm, val is_eq_to: Long) {

    val primaryInputs: MutableMap<Int, Var> = mutableMapOf()
    val auxInputs: MutableMap<Int, Var> = mutableMapOf()
    val auxInputHashes: MutableMap<Int, VarArray> = mutableMapOf()
    val auxInputNonces: MutableMap<Int, VarArray> = mutableMapOf()
    var output: Var? = null

    val r1cs: R1CSInstance = R1CSInstance()

    init {
        r1cs.isProver = isProver
        output = r1cs.mkPublicVal(is_eq_to)
        wire.populatePrimaryInputs()
        wire.setupAuxInputs()
        val o = wire.value()
        r1cs.AddEquality(o, output!!)
    }

    fun WireTerm.populatePrimaryInputs() {
        when (this) {
            is WireOp ->
                this.inputs.map { it.populatePrimaryInputs() }
            is WireIn -> {
                if (!auxInputHashes.containsKey(this.index)) {
                    auxInputHashes[this.index] = r1cs.mkPublicBitvec(mkByteBuf(this.hash.toByteArray()))
                }
                if (!auxInputNonces.containsKey(this.index)) {
                    auxInputNonces[this.index] = r1cs.mkPublicBitvec(mkByteBuf(this.nonce.toByteArray()))
                }
            }
            is WireDummyIn -> {
                if (!auxInputHashes.containsKey(this.index)) {
                    auxInputHashes[this.index] = r1cs.mkPublicBitvec(mkByteBuf(this.hash.toByteArray()))
                }
                if (!auxInputNonces.containsKey(this.index)) {
                    auxInputNonces[this.index] = r1cs.mkPublicBitvec(mkByteBuf(this.nonce.toByteArray()))
                }
            }
            is WireConst -> {
                if (!primaryInputs.containsKey(this.index)) {
                    primaryInputs[this.index] = r1cs.mkPublicVal(this.v.toLong())
                }
            }
        }
    }

    fun WireTerm.setupAuxInputs() {
        when (this) {
            is WireOp -> this.inputs.map { it.setupAuxInputs() }
            is WireIn -> {
                assert(isProver)
                if (!auxInputs.containsKey(this.index)) {
                    auxInputs[this.index] = r1cs.mkPrivateValProver(
                        this.v.toLong(),
                        auxInputHashes[this.index]!!,
                        auxInputNonces[this.index]!!,
                    )
                }
            }
            is WireDummyIn -> {
                assert(!isProver)
                if (!auxInputs.containsKey(this.index)) {
                    auxInputs[this.index] = r1cs.mkPrivateValVerifier(
                        auxInputHashes[this.index]!!,
                        auxInputNonces[this.index]!!,
                    )
                }
            }
            is WireConst -> {
            }
        }
    }

    fun WireTerm.value(): Var {
        return when (this) {
            is WireOp ->
                when (this.op) {
                    is Not -> r1cs.mkNot(this.inputs[0].value())
                    is And -> r1cs.mkAnd(inputs[0].value(), inputs[1].value())
                    is Or -> r1cs.mkOr(inputs[0].value(), inputs[1].value())
                    is Mux -> r1cs.mkMux(inputs[0].value(), inputs[1].value(), inputs[2].value())
                    is Multiplication -> r1cs.mkMult(inputs[0].value(), inputs[1].value())
                    is Addition -> r1cs.mkAdd(inputs[0].value(), inputs[1].value())
                    is EqualTo -> r1cs.mkEqualTo(inputs[0].value(), inputs[1].value())
                    is LessThan -> r1cs.mkLessThan(inputs[0].value(), inputs[1].value())
                    is LessThanOrEqualTo -> r1cs.mkLE(inputs[0].value(), inputs[1].value())
                    else -> throw java.lang.Exception("Unsupported op: $this")
                }

            is WireIn -> auxInputs[this.index]!!
            is WireDummyIn -> auxInputs[this.index]!!
            is WireConst -> primaryInputs[this.index]!!
        }
    }

    fun makeProof(pk: ByteBuf): ByteBuf {
        assert(output != null)
        return r1cs.generateProof(pk)
    }

    fun verifyProof(vk: ByteBuf, pf: ByteBuf): Boolean {
        assert(output != null)
        return r1cs.verifyProof(vk, pf)
    }

    fun genKeypair(): Keypair {
        assert(output != null)
        return r1cs.genKeypair()
    }
}

fun WireTerm.toR1CS(isProver: Boolean, is_eq_to: Int): R1CS =
    R1CS(isProver, this, is_eq_to.toLong())
