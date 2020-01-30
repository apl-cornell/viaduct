package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** The type of booleans. */
object BooleanType : ValueType {
    override val defaultValue: Value
        get() = BooleanValue(false)

    override fun toString(): String {
        return "bool"
    }
}
