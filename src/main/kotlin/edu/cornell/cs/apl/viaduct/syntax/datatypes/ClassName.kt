package edu.cornell.cs.apl.viaduct.syntax.datatypes

import edu.cornell.cs.apl.viaduct.syntax.Name

/** The name of a primitive or user-defined class. */
data class ClassName(override val name: String) : Name {
    override val nameCategory: String
        get() = "class"
}
