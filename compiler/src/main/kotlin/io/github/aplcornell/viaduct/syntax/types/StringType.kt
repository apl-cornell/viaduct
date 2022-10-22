package io.github.aplcornell.viaduct.syntax.types

import io.github.aplcornell.viaduct.syntax.values.StringValue
import io.github.aplcornell.viaduct.syntax.values.Value

/** The type of strings. */
object StringType : ValueType() {
    override val defaultValue: Value
        get() = StringValue("")

    override fun toString(): String {
        return "string"
    }
}
