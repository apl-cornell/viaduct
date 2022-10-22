package io.github.apl_cornell.viaduct.backend.IO

import io.github.apl_cornell.viaduct.syntax.values.Value

interface Strategy {
    suspend fun getInput(): Value
    suspend fun recvOutput(value: Value)
}
