package edu.cornell.cs.apl.viaduct.syntax

/**
 * A variable that binds base values.
 *
 * Temporaries are generated internally to name expression results.
 */
data class Temporary(override val name: String) : Variable {
    override val nameCategory: String
        get() = "temporary"
}
