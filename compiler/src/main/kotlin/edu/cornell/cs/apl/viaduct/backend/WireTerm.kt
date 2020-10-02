package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Negation
import edu.cornell.cs.apl.viaduct.syntax.operators.Not
import edu.cornell.cs.apl.viaduct.syntax.operators.Or
import edu.cornell.cs.apl.viaduct.syntax.operators.Subtraction

sealed class WireTerm
data class WireOp(val op: Operator, val inputs: List<WireTerm>) : WireTerm()
data class WireIn(val v: Int, val index: Int) : WireTerm()
data class WireDummyIn(val index: Int) : WireTerm()
data class WireConst(val v: Int) : WireTerm()

fun WireTerm.eval(): Int =
    when (this) {
        is WireOp ->
            when (this.op) {
                is Addition -> inputs[0].eval() + inputs[1].eval()
                is Subtraction -> inputs[0].eval() - inputs[1].eval()
                is Negation -> 0 - inputs[0].eval()
                is Multiplication -> inputs[0].eval() * inputs[1].eval()
                is And -> inputs[0].eval() * inputs[1].eval()
                is Not -> 1 - inputs[0].eval()
                is Or -> 1 - (1 - inputs[0].eval()) * (1 - inputs[1].eval())
                else -> throw Exception("unsupported op: $op")
            }
        is WireIn -> this.v
        is WireDummyIn -> throw Exception("evaluating dummy input")
        is WireConst -> this.v
    }

class WireGenerator {
    private var inIndex = 0
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
    fun mkConst(v: Int) = WireConst(v)
}
