package edu.cornell.cs.apl.viaduct.syntax.datatypes

import edu.cornell.cs.apl.viaduct.syntax.BinaryOperator

/** A class that stores a single modifiable value. */
val MutableCell: ClassName =
    ClassName("Cell")

/** A class that stores a list of mutable cells. */
val Vector: ClassName =
    ClassName("Array")

/** Returns the value stored in a mutable cell or at a specific index in an array. */
object Get : QueryName {
    override val name: String
        get() = "get"
}

/**
 * Replaces the value stored in a mutable cell or at a specific index in an array with a new value.
 */
object Set : UpdateName {
    override val name: String
        get() = "set"
}

/**
 * Applies a binary operator to the current value and the argument, and sets the stored value
 * to the result.
 */
data class Modify(val operator: BinaryOperator) : UpdateName {
    override val name: String
        get() = "$operator="
}
