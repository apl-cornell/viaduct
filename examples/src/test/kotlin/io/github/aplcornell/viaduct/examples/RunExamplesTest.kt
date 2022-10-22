package io.github.aplcornell.viaduct.examples

import io.github.aplcornell.viaduct.runtime.CombinedRuntime
import io.github.aplcornell.viaduct.runtime.ScannerIOStrategy
import io.github.aplcornell.viaduct.runtime.TCPNetworkStrategy
import io.github.aplcornell.viaduct.runtime.ViaductGeneratedProgram
import io.github.aplcornell.viaduct.syntax.Host
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.io.StringWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.Scanner

internal class RunExamplesTest {
    @ParameterizedTest
    @ArgumentsSource(ViaductProgramProvider::class)
    fun `produces correct output`(program: ViaductGeneratedProgram) {
        checkOutput(program)
    }

    @Disabled
    @ParameterizedTest
    @ArgumentsSource(ViaductABYProgramProvider::class)
    fun `aby program produces correct output`(program: ViaductGeneratedProgram) {
        checkOutput(program)
    }

    /** Convenience function that creates empty input/output files for new test programs. */
    @Disabled
    @ParameterizedTest
    @ArgumentsSource(ViaductProgramProvider::class)
    fun createEmptyInputOutputFiles(program: ViaductGeneratedProgram) {
        fun createFile(file: File) {
            println("Creating $file.")
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        program.hosts.forEach { host ->
            createFile(inputFile(program, host))
            createFile(outputFile(program, host))
        }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setLogLevel() {
            Configurator.setRootLevel(Level.TRACE)
        }
    }
}

/** Executes [program] and verifies that produces the expected output. */
private fun checkOutput(program: ViaductGeneratedProgram) {
    val outputs = program.run()

    program.hosts.forEach { host ->
        println("${host.name} outputs:")
        println(outputs.getValue(host))
    }

    program.hosts.forEach { host ->
        val expectedOutput = parseOutput(outputFile(program, host).readText())
        val actualOutput = parseOutput(outputs.getValue(host))
        assertEquals(expectedOutput, actualOutput, host.name)
    }
}

private fun parseOutput(output: String): List<String> =
    output.trim().split(Regex("\\s+"))

/** Runs [this] program as all hosts in parallel and returns their outputs. */
private fun ViaductGeneratedProgram.run(): Map<Host, String> {
    val hosts = this.hosts.sorted()
    val hostAddresses = hosts.associateWith {
        InetSocketAddress(InetAddress.getLoopbackAddress(), findAvailableTcpPort())
    }

    return runBlocking(Dispatchers.IO) {
        hosts.associateWith { host ->
            async { this@run.runAs(host, hostAddresses) }
        }.mapValues { it.value.await() }
    }
}

/** Runs [this] program as [host] and returns its output. */
private fun ViaductGeneratedProgram.runAs(host: Host, hostAddresses: Map<Host, InetSocketAddress>): String {
    Scanner(inputFile(this, host)).use { inputs ->
        val outputs = StringWriter()
        TCPNetworkStrategy(host, hostAddresses).use { networkStrategy ->
            networkStrategy.start()
            val runtime = CombinedRuntime(ScannerIOStrategy(inputs, outputs), networkStrategy)
            this.main(host, runtime)
            return outputs.toString()
        }
    }
}

private fun findAvailableTcpPort() =
    ServerSocket(0).use { it.localPort }

/** Returns the file containing [host]'s test inputs for [program]. */
private fun inputFile(program: ViaductGeneratedProgram, host: Host): File =
    File("inputs").resolve(fileSuffix(program, host))

/** Returns the file containing [host]'s expected outputs for [program]. */
private fun outputFile(program: ViaductGeneratedProgram, host: Host): File =
    File("outputs").resolve(fileSuffix(program, host))

private fun fileSuffix(program: ViaductGeneratedProgram, host: Host): String =
    "${program::class.qualifiedName!!.replace(".", File.separator)}-${host.name}.txt"
