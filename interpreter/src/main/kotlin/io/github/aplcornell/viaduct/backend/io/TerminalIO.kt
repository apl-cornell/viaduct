package io.github.aplcornell.viaduct.backend.io

import io.github.aplcornell.viaduct.syntax.values.IntegerValue
import io.github.aplcornell.viaduct.syntax.values.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.Scanner

private var logger = KotlinLogging.logger("TerminalIO")

class TerminalIO : Strategy {

    private val stdinScanner: Scanner = Scanner(System.`in`)

    override suspend fun getInput(): Value {
        return withContext(Dispatchers.IO) {
            // TODO: support booleans as well
            println("Input: ")
            IntegerValue(stdinScanner.nextInt())
        }
    }

    override suspend fun recvOutput(value: Value) {
        logger.info { "output $value" }
        withContext(Dispatchers.IO) { println(value) }
    }
}
