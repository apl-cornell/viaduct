package edu.cornell.cs.apl.viaduct.backend.IO

import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
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
