package edu.cornell.cs.apl.viaduct.examples

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import edu.cornell.cs.apl.viaduct.runtime.ScannerIOStrategy
import edu.cornell.cs.apl.viaduct.runtime.ViaductGeneratedProgram
import edu.cornell.cs.apl.viaduct.runtime.ViaductNetworkRuntime
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.version
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import java.io.File
import java.net.InetSocketAddress
import java.util.Scanner
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger("ExampleRunner")

private val generatedPrograms: Map<String, ViaductGeneratedProgram> =
    viaductPrograms.associateBy { program -> program::class.qualifiedName!! }

class ExampleRunner : NoOpCliktCommand(help = "Run compiled Viaduct programs.") {
    val verbose by option(
        "-v",
        "--verbose",
        help = """
            Print debugging information

            Repeat for more and more granular messages.
        """
    ).counted().validate {
        // Set the global logging level.
        // Note: this is not how `.validate` is meant to be used, but it's the closest feature Clikt provides.

        val level = when (it) {
            0 -> null
            1 -> Level.INFO
            2 -> Level.DEBUG
            3 -> Level.TRACE
            else -> Level.ALL
        }

        if (level != null) Configurator.setRootLevel(level)
    }

    init {
        versionOption(version)
        subcommands(List(), Run())
    }
}

class List : CliktCommand(help = "List compiled programs that can be executed.") {
    override fun run() {
        println("Found ${generatedPrograms.size} generated programs:")
        for (kv in generatedPrograms.entries) {
            println("- ${kv.key} with hosts ${kv.value.hosts.map { host -> host.name }}")
        }
    }
}

class Run : CliktCommand(help = "Run a compiled program for a single host.") {
    companion object {
        const val DEFAULT_IP: String = "127.0.0.1"
        const val DEFAULT_PORT: Int = 4000
    }

    private val programName by argument(
        "PROGRAM",
        help = "Program to run"
    )

    private val hostName by argument(
        "HOSTNAME",
        help = "Host that will run the protocol"
    )

    val inputFile: File? by option(
        "-i",
        "--input",
        help = "File to stream inputs from"
    ).file(canBeDir = false, mustExist = true)

    val hostAddresses: Map<String, String> by option(
        "-h",
        "--hostinfo",
        help = "Associate each host with its address"
    ).associate()

    override fun run() {
        val program = generatedPrograms[programName]
            ?: throw Error("Program $programName does not exist.")

        val host = Host(hostName)

        if (!program.hosts.contains(host)) {
            throw Error("Program $programName does not have host $hostName.")
        }

        val hostConnectionInfo: Map<Host, InetSocketAddress> =
            if (hostAddresses.size < program.hosts.size) {
                program.hosts
                    .mapIndexed { i, h -> h to InetSocketAddress.createUnresolved(DEFAULT_IP, DEFAULT_PORT + i) }
                    .toMap()
            } else {
                hostAddresses.map { kv ->
                    val otherHost = Host(kv.key)
                    val addressStr = kv.value.split(":", limit = 2)
                    val address = InetSocketAddress.createUnresolved(addressStr[0], addressStr[1].toInt())
                    otherHost to address
                }.toMap()
            }

        logger.info {
            "Running $programName as $hostName with host connection info: " +
                hostConnectionInfo.map { kv -> "${kv.key.name} => ${kv.value}" }.joinToString()
        }

        (inputFile?.let { Scanner(it) } ?: Scanner(System.`in`)).use { scanner ->
            val ioStrategy = ScannerIOStrategy(scanner)
            val runtime = ViaductNetworkRuntime(host, hostConnectionInfo, ioStrategy)
            runtime.start()
            program.main(host, runtime)
            runtime.shutdown()
        }
    }
}

fun main(args: Array<String>) =
    try {
        ExampleRunner().main(args)
    } catch (e: Throwable) {
        System.err.println(e.localizedMessage)
        exitProcess(1)
    }
