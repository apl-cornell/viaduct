package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** A type with a single element. */
object UnitType : ValueType() {
    override val defaultValue: Value = UnitValue

    override fun toString(): String {
        return "unit"
    }
}
