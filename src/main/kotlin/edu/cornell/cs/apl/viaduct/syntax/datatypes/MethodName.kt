package edu.cornell.cs.apl.viaduct.syntax.datatypes

import edu.cornell.cs.apl.viaduct.syntax.Name

/** An object method. */
interface MethodName : Name {
    override val nameCategory: String
        get() = "method"
}
