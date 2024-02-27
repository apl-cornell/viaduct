package io.github.aplcornell.viaduct.backend.io

import io.github.aplcornell.viaduct.syntax.values.Value

interface Strategy {
    suspend fun getInput(): Value

    suspend fun recvOutput(value: Value)
}
