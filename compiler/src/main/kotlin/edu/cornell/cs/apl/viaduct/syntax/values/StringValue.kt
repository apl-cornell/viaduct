package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.viaduct.syntax.types.StringType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** A string. */
data class StringValue(val value: String) : Value() {
    override val type: ValueType
        get() = StringType

    override fun toString(): String =
        "\"$value\""
}
