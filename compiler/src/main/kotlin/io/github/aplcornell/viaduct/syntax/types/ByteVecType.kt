package io.github.aplcornell.viaduct.syntax.types

import io.github.aplcornell.viaduct.syntax.values.ByteVecValue
import io.github.aplcornell.viaduct.syntax.values.Value

/** The type of booleans. */
object ByteVecType : ValueType() {
    override val defaultValue: Value
        get() = ByteVecValue(listOf())

    override fun toString(): String {
        return "bool"
    }
}
