package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.types.HostType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** A host. */
data class HostValue(val value: Host) : Value() {
    override val type: ValueType
        get() = HostType

    override fun toString(): String {
        return value.name
    }
}
