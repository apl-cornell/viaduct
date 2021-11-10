package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** A boolean value. */
data class BooleanValue(val value: Boolean) : Value() {
    override val type: ValueType
        get() = BooleanType

    override fun toString(): String {
        return value.toString()
    }
}
