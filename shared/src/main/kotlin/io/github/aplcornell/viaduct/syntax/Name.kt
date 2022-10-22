package io.github.aplcornell.viaduct.syntax

import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable

/** An object that names things. */
interface Name : PrettyPrintable {
    /** The given name. */
    val name: String

    /** Class of things this object names. */
    val nameCategory: String
}
