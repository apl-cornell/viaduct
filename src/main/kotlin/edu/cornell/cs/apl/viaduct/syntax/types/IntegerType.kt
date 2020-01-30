package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** The type of integers. */
object IntegerType : ValueType {
    override val defaultValue: Value
        get() = IntegerValue(0)

    override fun toString(): String {
        return "int"
    }
}
