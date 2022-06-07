package io.github.apl_cornell.viaduct.syntax.values

import io.github.apl_cornell.viaduct.syntax.types.ByteVecType
import io.github.apl_cornell.viaduct.syntax.types.ValueType

/** A bytevec. */
data class ByteVecValue(val value: List<Byte>) : Value() {
    override val type: ValueType
        get() = ByteVecType
}
