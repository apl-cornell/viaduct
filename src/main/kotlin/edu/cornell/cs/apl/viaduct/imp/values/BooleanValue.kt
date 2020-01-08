package edu.cornell.cs.apl.viaduct.imp.values

import edu.cornell.cs.apl.viaduct.imp.types.BooleanType
import edu.cornell.cs.apl.viaduct.imp.types.ValueType

/** A boolean. */
data class BooleanValue(val value: Boolean) : Value {
    override val type: ValueType
        get() = BooleanType

    override fun toString(): String {
        return value.toString()
    }
}
