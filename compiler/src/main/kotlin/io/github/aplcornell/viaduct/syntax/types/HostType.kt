package io.github.aplcornell.viaduct.syntax.types

import io.github.aplcornell.viaduct.syntax.values.HostValue
import io.github.aplcornell.viaduct.syntax.values.Value

/** The type assigned to [HostValue]s. */
object HostType : ValueType() {
    override val defaultValue: Value
        get() = throw UnsupportedOperationException()

    override fun toString(): String {
        return "host"
    }
}
