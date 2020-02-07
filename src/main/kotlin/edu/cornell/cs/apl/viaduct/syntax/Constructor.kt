package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** An object constructor. */
interface Constructor {
    val arguments: List<ValueType>
}
