package edu.cornell.cs.apl.viaduct.imp.ast2

/**
 * A wrapper for metadata information. Used to mark properties that should be ignored when checking
 * equality.
 *
 * All instances of this class compare equal to each other.
 *
 * @param T type of wrapped values
 */
class Metadata<T>(var data: T? = null) {
    override operator fun equals(other: Any?): Boolean {
        return other is Metadata<*>
    }

    override fun hashCode(): Int {
        return 0
    }

    override fun toString(): String {
        return data.toString()
    }
}
