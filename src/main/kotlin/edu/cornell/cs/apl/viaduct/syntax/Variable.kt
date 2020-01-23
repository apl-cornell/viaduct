package edu.cornell.cs.apl.viaduct.syntax

/** A variable is a name that stands for a value or an object instance. */
sealed class Variable : Name

/**
 * A variable that binds base values.
 *
 * Temporaries are generated internally to name expression results.
 */
data class Temporary(override val name: String) : Variable() {
    override val nameCategory: String
        get() = "temporary"
}

/** A variable that binds an object. */
data class ObjectVariable(override val name: String) : Variable() {
    override val nameCategory: String
        get() = "object"
}
