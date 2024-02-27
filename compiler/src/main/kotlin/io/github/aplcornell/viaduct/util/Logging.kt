package io.github.aplcornell.viaduct.util

import mu.KLogger
import kotlin.system.measureTimeMillis

/** Calls [operation] and returns the result. Additionally, logs its running time using [KLogger.info]. */
fun <R> KLogger.duration(
    operationName: String,
    operation: () -> R,
): R {
    this.debug { "Starting $operationName." }
    val result: R
    val time = measureTimeMillis { result = operation() }
    this.info { "Finished $operationName in $time ms." }
    return result
}
