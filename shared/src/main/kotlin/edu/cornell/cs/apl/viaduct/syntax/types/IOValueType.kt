package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** Type of values that can be sent to or received from hosts. */
sealed class IOValueType : ValueType()

/** The type of integers. */
object IntegerType : IOValueType() {
    override val defaultValue: Value
        get() = IntegerValue(0)

    override fun toString(): String {
        return "int"
    }
}

/** The type of booleans. */
object BooleanType : IOValueType() {
    override val defaultValue: Value
        get() = BooleanValue(false)

    override fun toString(): String {
        return "bool"
    }
}

/** A type with a single element. */
object UnitType : IOValueType() {
    override val defaultValue: Value = UnitValue

    override fun toString(): String {
        return "unit"
    }
}
