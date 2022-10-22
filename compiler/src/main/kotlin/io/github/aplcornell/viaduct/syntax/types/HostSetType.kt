package io.github.apl_cornell.viaduct.syntax.types

import io.github.apl_cornell.viaduct.syntax.values.HostSetValue
import io.github.apl_cornell.viaduct.syntax.values.Value

/** The type assigned to [HostSetValue]s. */
object HostSetType : ValueType() {
    override val defaultValue: Value
        get() = throw UnsupportedOperationException()

    override fun toString(): String {
        return "host set"
    }
}
