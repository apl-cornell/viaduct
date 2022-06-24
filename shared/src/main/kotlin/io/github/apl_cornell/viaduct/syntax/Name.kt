package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable

/** An object that names things. */
interface Name : PrettyPrintable {
    /** The given name. */
    val name: String

    /** Class of things this object names. */
    val nameCategory: String
}
