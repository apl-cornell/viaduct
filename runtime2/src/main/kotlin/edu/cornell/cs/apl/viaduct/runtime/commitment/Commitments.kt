package edu.cornell.cs.apl.viaduct.runtime.commitment

import edu.cornell.cs.apl.viaduct.runtime.commitment.Committed.Companion.commitment
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.security.MessageDigest
import java.security.SecureRandom

/** A value committed to. Packages the value and the secret needed to open the [Commitment]. */
@Serializable
class Committed<T> private constructor(val value: T, val nonce: ByteArray) {
    constructor(value: T) : this(
        value = value,
        nonce = ByteArray(NONCE_LENGTH).apply {
            secureRandom.nextBytes(this)
        }
    )

    companion object {
        /** Size of each nonce in bytes. */
        const val NONCE_LENGTH = 256 / 8

        private val secureRandom = SecureRandom.getInstanceStrong()

        /** A statically fixed nonce that is not random at all. Used to create fake commitments. */
        private val fakeNonce = ByteArray(NONCE_LENGTH)

        @OptIn(ExperimentalSerializationApi::class)
        inline fun <reified T> Committed<T>.commitment(): Commitment<T> =
            MessageDigest.getInstance("SHA-256").run {
                update(ProtoBuf.encodeToByteArray(value))
                update(nonce)
                Commitment(digest())
            }

        /**
         * A commitment to a known value. Note that the returned commitment has no hiding property.
         *
         * Can be used for code uniformity in places that require a [Committed] object,
         * but when there is no need to hide the value.
         */
        fun <T> fake(value: T): Committed<T> =
            Committed(value, fakeNonce)
    }
}

/** A commitment to a value of type [T]. Can be opened to receive the committed value. */
@Serializable
class Commitment<T>(val hash: ByteArray) {
    /** */
    inline fun <reified T> open(secret: Committed<T>): T =
        if (secret.nonce.size != Committed.NONCE_LENGTH || !hash.contentEquals(secret.commitment().hash))
            throw InvalidCommitmentException(secret.value)
        else
            secret.value
}
