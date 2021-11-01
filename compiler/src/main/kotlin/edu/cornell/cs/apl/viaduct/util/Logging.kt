package edu.cornell.cs.apl.viaduct.util

import mu.KLogger
import kotlin.system.measureTimeMillis

/** Calls [operation] and logs its running time using [KLogger.info]. */
fun KLogger.duration(operationName: String, operation: () -> Unit) {
    this.info { "Starting $operationName." }
    val time = measureTimeMillis(operation)
    this.info { "Finished $operationName in $time ms." }
}
