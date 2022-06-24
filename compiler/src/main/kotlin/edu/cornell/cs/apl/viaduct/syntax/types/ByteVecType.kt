package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** The type of booleans. */
object ByteVecType : ValueType() {
    override val defaultValue: Value
        get() = ByteVecValue(listOf())

    override fun toString(): String {
        return "bool"
    }
}
