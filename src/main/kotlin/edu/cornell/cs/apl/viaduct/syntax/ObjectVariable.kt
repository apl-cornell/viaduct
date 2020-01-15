package edu.cornell.cs.apl.viaduct.syntax

/** A variable that binds an object. */
data class ObjectVariable(override val name: String) : Variable {
    override val nameCategory: String
        get() = "object"
}
