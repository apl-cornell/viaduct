package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.backend.commitment.Hashing
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThan
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThanOrEqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Not
import edu.cornell.cs.apl.viaduct.syntax.operators.Or

sealed class WireTerm
data class WireOp(val op: Operator, val inputs: List<WireTerm>) : WireTerm()
data class WireIn(val v: Int, val index: Int) : WireTerm()
data class WireDummyIn(val index: Int) : WireTerm()
data class WireConst(val index: Int, val v: Int) : WireTerm()

fun String.asPrettyPrintable(): PrettyPrintable = Document(this)

// For booleans, encoding is: 0 if false, anything else if true

fun WireTerm.eval(): Int =
    when (this) {
        is WireOp ->
            when (this.op) {
                is Addition -> inputs[0].eval() + inputs[1].eval()
                is Multiplication -> inputs[0].eval() * inputs[1].eval()
                is And -> inputs[0].eval() * inputs[1].eval()
                is Not -> 1 - inputs[0].eval()
                is Or -> 1 - (1 - inputs[0].eval()) * (1 - inputs[1].eval())
                is EqualTo -> if (inputs[0].eval() == inputs[1].eval()) {
                    1
                } else {
                    0
                }
                is Mux -> if (inputs[0].eval() == 1) (inputs[1].eval()) else (inputs[2].eval())
                is LessThan -> if (inputs[0].eval() < inputs[1].eval()) (1) else (0)
                is LessThanOrEqualTo -> if (inputs[0].eval() <= inputs[1].eval()) (1) else (0)
                else -> throw Exception("unsupported op: $op")
            }
        is WireIn -> this.v
        is WireDummyIn -> throw Exception("evaluating dummy input")
        is WireConst -> this.v
    }

class WireGenerator {
    private var inIndex = 0
    private var constIndex = 0
    fun mkOp(op: Operator, inputs: List<WireTerm>) = WireOp(op, inputs)
    fun mkDummyIn(): WireTerm {
        val r = WireDummyIn(inIndex)
        inIndex++
        return r
    }

    fun mkIn(v: Int): WireTerm {
        val r = WireIn(v, inIndex)
        inIndex++
        return r
    }

    fun mkConst(v: Int): WireTerm {
        val r = WireConst(constIndex, v)
        constIndex++
        return r
    }
}

// // Canonical naming for WireTerms

fun WireTerm.asString(): String {
    return when (this) {
        is WireConst -> "Aux($index)"
        is WireDummyIn -> "In($index)"
        is WireIn -> "In($index)"
        is WireOp -> op.asDocument(inputs.map { it.asString().asPrettyPrintable() }).print()
    }
}
fun WireTerm.hash(): String {
    val blist = Hashing.deterministicHash(this.asString().toByteArray().toList()).hash
    return blist.hashCode().toString(16)
}

data class NormalizeCounter(
    var inIndex: Int = 0,
    val inMap: MutableMap<Int, Int>,
    var publicIndex: Int = 0,
    val publicMap: MutableMap<Int, Int>
)

fun WireTerm.normalize(counter: NormalizeCounter): WireTerm =
    when (this) {
        is WireOp ->
            WireOp(this.op, this.inputs.map { it.normalize(counter) })
        is WireIn -> {
            if (counter.inMap[this.index] == null) {
                val i = counter.inIndex
                counter.inMap[this.index] = i
                counter.inIndex++
                WireIn(this.v, i)
            } else {
                WireIn(this.v, counter.inMap[this.index]!!)
            }
        }
        is WireDummyIn -> {
            if (counter.inMap[this.index] == null) {
                val i = counter.inIndex
                counter.inMap[this.index] = i
                counter.inIndex++
                WireDummyIn(i)
            } else {
                WireDummyIn(counter.inMap[this.index]!!)
            }
        }
        is WireConst -> {
            if (counter.publicMap[this.index] == null) {
                val i = counter.publicIndex
                counter.publicMap[this.index] = i
                counter.publicIndex++
                WireConst(i, this.v)
            } else {
                WireConst(counter.publicMap[this.index]!!, this.v)
            }
        }
    }

fun WireTerm.wireName(): String =
    this.normalize(NormalizeCounter(0, mutableMapOf(), 0, mutableMapOf())).hash()
