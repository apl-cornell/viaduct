package io.github.apl_cornell.viaduct.algebra

/**
 * Like [Comparable], but not all pairs of elements have to be ordered.
 *
 * @param T the type of objects this object may be compared to.
 */
interface PartialOrder<T> {
    /**
     * Returns true if `this` is ordered before [that].
     *
     * It is not necessary that either `this.lessThanOrEqualTo(that)` or `that.lessThanOrEqualTo(this)`.
     */
    fun lessThanOrEqualTo(that: T): Boolean
}
