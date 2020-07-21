package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.values.StringValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** The type of strings. */
object StringType : ValueType() {
    override val defaultValue: Value
        get() = StringValue("")

    override fun toString(): String {
        return "string"
    }
}
