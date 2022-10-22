package io.github.aplcornell.viaduct.attributes

/**
 * A mutable cell that stores a partial result, which can be updated and/or finalized.
 *
 * The cell starts with an initial value. Each update to the cell's value should move the value
 * "up" in some lattice. At some point, the final result is computed, and the cell is marked as
 * final. No further updates are allowed after this point.
 */
internal class AttributeValue<T>(initial: T) {
    /** The current (not necessarily finalized) value of the cell. */
    var currentValue = initial
        /** @throws IllegalStateException if the value is set when [isFinalized] is `true`. */
        set(value) {
            if (isFinalized) {
                throw IllegalStateException("Cannot set the value of a finalized cell.")
            }
            field = value
        }

    /** True when the value of the cell is fully computed and will no longer be updated. */
    var isFinalized: Boolean = false
        private set

    /**
     * Whether the cell value has already been accessed/updated in this iteration of some algorithm.
     * Useful when checking for circularity.
     */
    var isVisited: Boolean = false

    /**
     * The finalized value of the cell.
     *
     * @throws IllegalStateException if the value is accessed when [isFinalized] is `false`.
     */
    val value: T
        get() =
            if (isFinalized) {
                currentValue
            } else {
                throw IllegalStateException("Cannot get the value of a non-finalized cell.")
            }

    /** Marks the cell as finalized. */
    fun finalize() {
        isFinalized = true
    }
}
