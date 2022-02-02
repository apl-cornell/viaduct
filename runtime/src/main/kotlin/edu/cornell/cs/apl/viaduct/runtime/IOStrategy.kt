package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.types.IOValueType
import edu.cornell.cs.apl.viaduct.syntax.values.IOValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

interface IOStrategy {
    fun input(type: IOValueType): Value
    fun output(value: IOValue)
}
