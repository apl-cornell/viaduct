package edu.cornell.cs.apl.viaduct.syntax

/** Objects that name things. */
interface Name {
    /** The given name. */
    val name: String

    /** Class of things this objects names. */
    val nameCategory: String
}
