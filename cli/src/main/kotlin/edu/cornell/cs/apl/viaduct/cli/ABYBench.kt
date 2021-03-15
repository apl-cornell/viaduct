package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import com.github.ajalt.clikt.parameters.types.file
import edu.cornell.cs.apl.viaduct.backend.aby.ABYBenchRunner
import java.util.Scanner
import kotlin.system.measureTimeMillis
import mu.KotlinLogging

private var logger = KotlinLogging.logger("ABYBench")

// Benchmark programs using "bare" ABY API.
class ABYBench : CliktCommand(help = "Benchmark bare ABY programs") {
    private val hostName by argument("HOSTNAME", help = "Host that will run the protocol")

    private val benchmark by argument("BENCHMARK", help = "Benchmark to run")

    val hostAddress: List<Pair<String, String>> by option(
        "-h",
        "--host",
        help = "Set host connection info"
    ).pair().multiple()

    private val input by
        argument(
            "FILE",
            help = "Read input program from <file> (default: stdin)"
        ).file(mustExist = true, canBeDir = false, mustBeReadable = true)

    override fun run() {
        val inputScanner = Scanner(input)
        val benchRunner = ABYBenchRunner(hostName, hostAddress.toMap(), benchmark, inputScanner)
        val duration = measureTimeMillis { benchRunner.run() }
        logger.info { "benchmark duration: ${duration}ms" }
    }
}
