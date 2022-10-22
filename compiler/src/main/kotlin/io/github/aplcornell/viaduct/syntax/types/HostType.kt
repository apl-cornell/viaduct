package io.github.apl_cornell.viaduct.syntax.types

import io.github.apl_cornell.viaduct.syntax.values.HostValue
import io.github.apl_cornell.viaduct.syntax.values.Value

/** The type assigned to [HostValue]s. */
object HostType : ValueType() {
    override val defaultValue: Value
        get() = throw UnsupportedOperationException()

    override fun toString(): String {
        return "host"
    }
}
