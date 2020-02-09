package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** The type of update return values. */
object UnitType : ValueType {
    override val defaultValue: Value = UnitValue

    override fun toString(): String {
        return "unit"
    }
}
