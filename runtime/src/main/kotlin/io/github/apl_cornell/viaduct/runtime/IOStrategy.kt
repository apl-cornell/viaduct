package io.github.apl_cornell.viaduct.runtime

import io.github.apl_cornell.viaduct.syntax.types.IOValueType
import io.github.apl_cornell.viaduct.syntax.values.IOValue
import io.github.apl_cornell.viaduct.syntax.values.Value

interface IOStrategy {
    /** Inputs a value of type [type] from the host. */
    fun input(type: IOValueType): Value

    /** Outputs [value] to the host. */
    fun output(value: IOValue)
}
