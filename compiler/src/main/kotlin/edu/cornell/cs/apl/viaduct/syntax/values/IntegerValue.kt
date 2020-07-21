package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** An integer. */
data class IntegerValue(val value: Int) : Value() {
    override val type: ValueType
        get() = IntegerType

    override fun toString(): String {
        return value.toString()
    }
}
