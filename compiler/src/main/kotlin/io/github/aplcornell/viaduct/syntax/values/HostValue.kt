package io.github.aplcornell.viaduct.syntax.values

import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.types.HostType
import io.github.aplcornell.viaduct.syntax.types.ValueType

/** A host. */
data class HostValue(val value: Host) : Value() {
    override val type: ValueType
        get() = HostType

    override fun toString(): String {
        return value.name
    }
}
