package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.types.IOValueType
import edu.cornell.cs.apl.viaduct.syntax.values.IOValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

interface IOStrategy {
    /** Inputs a value of type [type] from the host. */
    fun input(type: IOValueType): Value

    /** Outputs [value] to the host. */
    fun output(value: IOValue)
}
