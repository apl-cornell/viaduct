package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.io.Serializable

interface Runtime {
    suspend fun input(type: ValueType): Value

    suspend fun output(value: Value)

    suspend fun send(value: Serializable, receiver: Host)

    suspend fun receive(sender: Host): Serializable
}
