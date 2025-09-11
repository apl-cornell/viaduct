package io.github.aplcornell.viaduct.syntax.values

import io.github.aplcornell.viaduct.syntax.types.StringType
import io.github.aplcornell.viaduct.syntax.types.ValueType

/** A string. */
data class StringValue(val value: String) : Value() {
    override val type: ValueType
        get() = StringType

    override fun toString(): String = "\"$value\""
}
