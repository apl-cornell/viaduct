package io.github.aplcornell.viaduct.runtime.aby

import io.github.apl_cornell.aby.Aby
import io.github.apl_cornell.aby.Circuit
import io.github.apl_cornell.aby.Share
import io.github.apl_cornell.aby.UInt32Vector
import java.math.BigInteger

/** Implements bitwise not */
fun Circuit.putNOTGate(input: Share): Share {
    val inverses = mutableListOf<Long>()
    inverses.addAll(input.wires)
    inverses[0] = putINVGate(inverses.first())
    return Aby.createNewShare(UInt32Vector(inverses), this)
}

fun Circuit.secretIndexQuery(
    indexValue: Share,
    shareVector: Array<Share>,
): Share {
    // return 0 in case of indexing error
    var currentShare = this.putCONSGate(BigInteger.ZERO, 32)
    for (i in shareVector.indices) {
        val guard = this.putEQGate(indexValue, this.putCONSGate(i.toBigInteger(), 32))
        val mux = this.putMUXGate(guard, shareVector[i], currentShare)
        currentShare = mux
    }
    return currentShare
}

fun Array<Share>.secretUpdateModify(
    circuit: Circuit,
    index: Share,
    operation: (Share) -> Share,
) {
    for (i in this.indices) {
        val rhs = operation(this[i])
        val guard = circuit.putEQGate(index, circuit.putCONSGate(i.toBigInteger(), 32))
        val mux = circuit.putMUXGate(this[i], rhs, guard)
        this[i] = mux
    }
}

fun Array<Share>.secretUpdateSet(
    circuit: Circuit,
    index: Share,
    argument: Share,
) {
    for (i in this.indices) {
        val guard = circuit.putEQGate(index, circuit.putCONSGate(i.toBigInteger(), 32))
        val mux = circuit.putMUXGate(this[i], argument, guard)
        this[i] = mux
    }
}

val Long.bool: Boolean get() = this != 0.toLong()
