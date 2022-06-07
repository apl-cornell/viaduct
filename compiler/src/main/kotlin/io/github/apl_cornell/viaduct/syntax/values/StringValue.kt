package io.github.apl_cornell.viaduct.syntax.values

import io.github.apl_cornell.viaduct.syntax.types.StringType
import io.github.apl_cornell.viaduct.syntax.types.ValueType

/** A string. */
data class StringValue(val value: String) : Value() {
    override val type: ValueType
        get() = StringType

    override fun toString(): String =
        "\"$value\""
}
