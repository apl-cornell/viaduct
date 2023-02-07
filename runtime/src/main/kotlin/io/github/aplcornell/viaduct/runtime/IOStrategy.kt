package io.github.aplcornell.viaduct.runtime

import io.github.aplcornell.viaduct.syntax.types.IOValueType
import io.github.aplcornell.viaduct.syntax.values.IOValue
import io.github.aplcornell.viaduct.syntax.values.Value

interface IOStrategy {
    /** Inputs a value of type [type] from the host. */
    fun input(type: IOValueType): Value

    /** Outputs [value] to the host. */
    fun output(value: IOValue)
}
