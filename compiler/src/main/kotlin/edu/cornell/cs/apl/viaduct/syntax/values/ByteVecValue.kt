package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.viaduct.syntax.types.ByteVecType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** A bytevec. */
data class ByteVecValue(val value: List<Byte>) : Value() {
    override val type: ValueType
        get() = ByteVecType
}
