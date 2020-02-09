package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.viaduct.syntax.types.UnitType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** A unit, returned by declarations and updates. */
object UnitValue : Value {
    override val type: ValueType
        get() = UnitType

    override fun toString(): String {
        return "unit"
    }
}
