package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** The type assigned to [HostSetValue]s. */
object HostSetType : ValueType() {
    override val defaultValue: Value
        get() = throw UnsupportedOperationException()

    override fun toString(): String {
        return "host set"
    }
}
