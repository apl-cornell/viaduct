package edu.cornell.cs.apl.viaduct.syntax

/** An object that names things. */
interface Name {
    /** The given name. */
    val name: String

    /** Class of things this object names. */
    val nameCategory: String
}
