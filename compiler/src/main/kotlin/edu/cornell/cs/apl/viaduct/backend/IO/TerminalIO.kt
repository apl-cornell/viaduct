package edu.cornell.cs.apl.viaduct.backend.IO

import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
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
        logger.info{ "output $value"}
        withContext(Dispatchers.IO) { println(value) }
    }
}
