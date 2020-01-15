package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** The result of evaluating an [ExpressionNode]. */
interface Value {
    /** The type of the value. */
    val type: ValueType
}
