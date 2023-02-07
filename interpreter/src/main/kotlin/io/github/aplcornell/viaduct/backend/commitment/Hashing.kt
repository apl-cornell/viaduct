package io.github.aplcornell.viaduct.backend.commitment

import io.github.aplcornell.viaduct.backend.CleartextClassObject
import io.github.aplcornell.viaduct.backend.ImmutableCellObject
import io.github.aplcornell.viaduct.backend.MutableCellObject
import io.github.aplcornell.viaduct.backend.NullObject
import io.github.aplcornell.viaduct.backend.VectorObject
import io.github.aplcornell.viaduct.syntax.values.BooleanValue
import io.github.aplcornell.viaduct.syntax.values.ByteVecValue
import io.github.aplcornell.viaduct.syntax.values.IntegerValue
import io.github.aplcornell.viaduct.syntax.values.StringValue
import io.github.aplcornell.viaduct.syntax.values.UnitValue
import io.github.aplcornell.viaduct.syntax.values.Value
import java.security.MessageDigest
import java.security.SecureRandom

data class HashInfo(val hash: List<Byte>, val nonce: List<Byte>) {
    fun verify(data: List<Byte>): Boolean {
        return (
            MessageDigest.getInstance("SHA-256").digest(nonce.toByteArray() + data.toByteArray()).toList()
                ==
                hash
            )
    }
}

fun Boolean.toByte(): Byte = if (this) 1 else 0

fun Value.encode(): List<Byte> {
    return when (this) {
        is IntegerValue -> this.value.toBigInteger().toByteArray().toList()
        is BooleanValue -> listOf(this.value.toByte())
        is ByteVecValue -> this.value
        is UnitValue -> listOf()
        is StringValue -> this.value.toByteArray().toList()
        else -> throw Error("Unknown value!")
    }
}

fun CleartextClassObject.encode(): List<Byte> {
    return when (this) {
        is ImmutableCellObject -> this.value.encode()
        is MutableCellObject -> this.value.encode()
        is VectorObject -> TODO("Vector encoding")
        is NullObject -> listOf()
    }
}

fun genNonce(width: Int): List<Byte> {
    val nonce = ByteArray(width)
    SecureRandom().nextBytes(nonce)
    return nonce.toList()
}

object Hashing {
    private fun generateHash(data: List<Byte>): HashInfo {
        val nonce = ByteArray(16)
        SecureRandom().nextBytes(nonce)
        return HashInfo(
            MessageDigest.getInstance("SHA-256").digest(nonce + data.toByteArray()).toList(),
            nonce.toList(),
        )
    }

    fun generateHash(v: Value): HashInfo = generateHash(v.encode())

    fun generateHash(c: CleartextClassObject): HashInfo = generateHash(c.encode())

    /** Deterministic hash for storing literals. **/

    fun deterministicHash(data: List<Byte>): HashInfo {
        return HashInfo(
            MessageDigest.getInstance("SHA-256").digest(data.toByteArray()).toList(),
            listOf(),
        )
    }

    fun deterministicHash(v: Value): HashInfo = deterministicHash(v.encode())
}
