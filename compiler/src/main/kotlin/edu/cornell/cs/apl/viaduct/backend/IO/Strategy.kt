package edu.cornell.cs.apl.viaduct.backend.IO

import edu.cornell.cs.apl.viaduct.syntax.values.Value

interface Strategy {
    suspend fun getInput(): Value
    suspend fun recvOutput(value: Value)
}
