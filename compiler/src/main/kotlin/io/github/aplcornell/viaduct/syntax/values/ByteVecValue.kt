package io.github.aplcornell.viaduct.syntax.values

import io.github.aplcornell.viaduct.syntax.types.ByteVecType
import io.github.aplcornell.viaduct.syntax.types.ValueType

/** A bytevec. */
data class ByteVecValue(val value: List<Byte>) : Value() {
    override val type: ValueType
        get() = ByteVecType
}
