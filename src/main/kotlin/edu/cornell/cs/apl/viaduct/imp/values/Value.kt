package edu.cornell.cs.apl.viaduct.imp.values

import edu.cornell.cs.apl.viaduct.imp.ast2.ExpressionNode
import edu.cornell.cs.apl.viaduct.imp.types.ValueType

/** The result of evaluating an [ExpressionNode]. */
interface Value {
    /** The type of the value. */
    val type: ValueType
}
