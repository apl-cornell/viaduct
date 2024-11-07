package io.github.aplcornell.viaduct.backend.io

import io.github.aplcornell.viaduct.syntax.values.IntegerValue
import io.github.aplcornell.viaduct.syntax.values.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Scanner

class FileStrategy(inFile: File) : Strategy {
    private val scanner = Scanner(inFile)

    override suspend fun getInput(): Value {
        return IntegerValue(scanner.nextInt())
    }

    override suspend fun recvOutput(value: Value) {
        withContext(Dispatchers.IO) { println(value) }
    }
}
