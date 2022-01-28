package edu.cornell.cs.apl.viaduct.codegeneration

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import edu.cornell.cs.apl.viaduct.runtime.FileIOStrategy
import edu.cornell.cs.apl.viaduct.runtime.TerminalIOStrategy
import edu.cornell.cs.apl.viaduct.runtime.ViaductGeneratedProgram
import edu.cornell.cs.apl.viaduct.runtime.ViaductNetworkRuntime
import edu.cornell.cs.apl.viaduct.syntax.Host
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import java.io.File
import java.net.InetSocketAddress

private val logger = KotlinLogging.logger("RunCodegenExamples")

class CodegenRunnerCommand : CliktCommand(
    help = "Run compiled Viaduct programs.",
    invokeWithoutSubcommand = true
) {

    val verbose by
    option(
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

    override fun run() {
        val generatedPrograms: Map<String, ViaductGeneratedProgram> =
            viaductPrograms.map { program -> program::class.qualifiedName!! to program }.toMap()
        currentContext.obj = generatedPrograms
    }
}

class CodegenRunnerListCommand : CliktCommand(
    name = "list",
    help = "List known compiled programs that can be executed"
) {
    val programMap by requireObject<Map<String, ViaductGeneratedProgram>>()

    override fun run() {
        println("Found ${programMap.size} generated programs:")
        for (kv in programMap.entries) {
            println("- ${kv.key} with hosts ${kv.value.hosts.map { host -> host.name }}")
        }
    }
}

class CodegenRunnerRunCommand : CliktCommand(
    name = "run",
    help = "Run compiled protocol for a single host"
) {
    companion object {
        val DEFAULT_IP: String = "127.0.0.1"
        val DEFAULT_PORT: Int = 4000
    }

    private val programName by argument(
        "PROGRAM",
        help = "Program to run"
    )

    private val hostName by argument(
        "HOSTNAME",
        help = "Host that will run the protocol."
    )

    val inputFile: File? by option(
        "-in",
        "--input",
        help = "File to stream inputs from"
    ).file(canBeDir = false, mustExist = true)

    val hostAddresses: Map<String, String> by option(
        "-hi", "--hostinfo"
    ).associate()

    val programMap by requireObject<Map<String, ViaductGeneratedProgram>>()

    override fun run() {
        val program = programMap.get(programName)
            ?: throw Error("Program $programName does not exist")

        val host = Host(hostName)

        if (!program.hosts.contains(host)) {
            throw Error("Program $programName does not have host $hostName")
        }

        val hostConnectionInfo: Map<Host, InetSocketAddress> =
            if (hostAddresses.size < program.hosts.size) {
                program.hosts.sorted()
                    .zip(DEFAULT_PORT..(DEFAULT_PORT + program.hosts.size))
                    .map { kv -> kv.first to InetSocketAddress.createUnresolved(DEFAULT_IP, kv.second) }
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
            "running $programName as $hostName with host connection info: " +
                hostConnectionInfo.map { kv -> "${kv.key.name} => ${kv.value}" }.joinToString()
        }

        // create IO strategy
        val ioStrategy = inputFile?.let { FileIOStrategy(it) } ?: TerminalIOStrategy()

        val runtime = ViaductNetworkRuntime(host, hostConnectionInfo, ioStrategy)
        runtime.start()
        program.main(host, runtime)
        runtime.shutdown()
    }
}

fun main(args: Array<String>) =
    CodegenRunnerCommand()
        .subcommands(CodegenRunnerRunCommand(), CodegenRunnerListCommand())
        .main(args)
