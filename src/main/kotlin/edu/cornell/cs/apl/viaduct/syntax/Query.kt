package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** A read-only method that returns information about the object without modifying it. */
interface Query : Method {
    val result: ValueType
}
