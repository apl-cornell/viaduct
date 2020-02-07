package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** An object method. */
interface Method {
    /** Number of arguments the method takes. */
    val arity: Int
    val arguments: List<ValueType>
}
