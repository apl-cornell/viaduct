package io.github.apl_cornell.viaduct.syntax.datatypes

import io.github.apl_cornell.viaduct.syntax.BinaryOperator

/** A class that stores a single unmodifiable value. */
val ImmutableCell: ClassName =
    ClassName("ImmutableVariable")

/** A class that stores a single modifiable value. */
val MutableCell: ClassName =
    ClassName("MutableVariable")

/** A class that stores a list of mutable cells. */
val Vector: ClassName =
    ClassName("Array")

/** Returns the value stored at a given index in a container. */
object Get : QueryName {
    override val name: String
        get() = "get"
}

/** Replaces the value stored at a given index in a container with a new value. */
object Set : UpdateName {
    override val name: String
        get() = "set"
}

/**
 * Applies a binary operator to the current value and the given argument, and sets the stored value
 * to the result.
 */
data class Modify(val operator: BinaryOperator) : UpdateName {
    override val name: String
        get() = "$operator="
}
