package io.github.apl_cornell.viaduct.syntax.values

import io.github.apl_cornell.viaduct.syntax.types.BooleanType
import io.github.apl_cornell.viaduct.syntax.types.IOValueType
import io.github.apl_cornell.viaduct.syntax.types.IntegerType
import io.github.apl_cornell.viaduct.syntax.types.UnitType

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
