package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.PrettyPrintable

/** An object that names things. */
interface Name : PrettyPrintable {
    /** The given name. */
    val name: String

    /** Class of things this object names. */
    val nameCategory: String
}
