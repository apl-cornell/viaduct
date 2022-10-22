package io.github.apl_cornell.viaduct.syntax.values

import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.types.HostType
import io.github.apl_cornell.viaduct.syntax.types.ValueType

/** A host. */
data class HostValue(val value: Host) : Value() {
    override val type: ValueType
        get() = HostType

    override fun toString(): String {
        return value.name
    }
}
