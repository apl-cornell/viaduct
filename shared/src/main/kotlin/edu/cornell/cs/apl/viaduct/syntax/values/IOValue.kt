package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.IOValueType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.UnitType

sealed class IOValue : Value()

/** An integer. */
data class IntegerValue(val value: Int) : IOValue() {
    override val type: IOValueType
        get() = IntegerType

    override fun toString(): String {
        return value.toString()
    }
}

/** A boolean value. */
data class BooleanValue(val value: Boolean) : IOValue() {
    override val type: IOValueType
        get() = BooleanType

    override fun toString(): String {
        return value.toString()
    }
}

/** The unique value of type [UnitType]. */
object UnitValue : IOValue() {
    override val type: IOValueType
        get() = UnitType

    override fun toString(): String {
        return "unit"
    }
}
