package io.github.apl_cornell.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import io.github.apl_cornell.viaduct.backend.CleartextProtocolInterpreter
import io.github.apl_cornell.viaduct.backend.HostAddress
import io.github.apl_cornell.viaduct.backend.IO.FileStrategy
import io.github.apl_cornell.viaduct.backend.IO.Strategy
import io.github.apl_cornell.viaduct.backend.IO.TerminalIO
import io.github.apl_cornell.viaduct.backend.ProtocolBackend
import io.github.apl_cornell.viaduct.backend.ViaductBackend
import io.github.apl_cornell.viaduct.backend.aby.ABYProtocolInterpreter
import io.github.apl_cornell.viaduct.backend.commitment.CommitmentProtocolInterpreterFactory
import io.github.apl_cornell.viaduct.backend.zkp.ZKPProtocolInterpreterFactory
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.parsing.parse
import io.github.apl_cornell.viaduct.passes.elaborated
import io.github.apl_cornell.viaduct.syntax.Host
import java.io.File
import kotlin.system.exitProcess

class Run : CliktCommand(help = "Run compiled protocol for a single host") {
    private val hostName by
    argument(
        "HOSTNAME",
        help = "Host that will run the protocol."
    )

    val input: File? by inputProgram()

    val inputStrategy by option(
        "-in",
        "--input",
        help = "File to stream inputs from"
    ).file(canBeDir = false, mustExist = false)

    val hostAddress: List<Pair<String, String>> by option(
        "-h",
        "--host",
        help = "Set host connection info"
    ).pair().multiple()

    val abyPort: Int? by option(
        "--abyport",
        help = "Set port used by ABY"
    ).int()

    private fun getProtocolBackends(): List<ProtocolBackend> {
        return listOf(
            CleartextProtocolInterpreter,
            ABYProtocolInterpreter,
            CommitmentProtocolInterpreterFactory,
            ZKPProtocolInterpreterFactory
        )
    }

    override fun run() {
        val program = input.sourceFile().parse(DefaultCombinedBackend.protocolParsers).elaborated()

        val connectionInfoMap: Map<Host, HostAddress> =
            hostAddress.associate { kv ->
                val host = Host(kv.first)
                val addressStr = kv.second.split(":", limit = 2)
                val address = HostAddress(addressStr[0], addressStr[1].toInt())
                host to address
            }

        if (abyPort != null) {
            ABYProtocolInterpreter.port = abyPort as Int
        }

        val backend = ViaductBackend(getProtocolBackends(), connectionInfoMap)

        val strategy: Strategy =
            if (inputStrategy == null)
                (TerminalIO())
            else
                (FileStrategy(inputStrategy!!))

        // interpret program
        backend.run(program, Host(hostName), strategy)
        exitProcess(0)
    }
}
