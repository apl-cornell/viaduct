package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import edu.cornell.cs.apl.viaduct.backend.HostAddress
import edu.cornell.cs.apl.viaduct.backend.IO.FileStrategy
import edu.cornell.cs.apl.viaduct.backend.IO.Strategy
import edu.cornell.cs.apl.viaduct.backend.IO.TerminalIO
import edu.cornell.cs.apl.viaduct.backend.PlaintextProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ViaductBackend
import edu.cornell.cs.apl.viaduct.backend.aby.ABYProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.commitment.CommitmentProtocolInterpreterFactory
import edu.cornell.cs.apl.viaduct.backend.zkp.ZKPProtocolInterpreterFactory
import edu.cornell.cs.apl.viaduct.parsing.ArithABYProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.BoolABYProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.CommitmentProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.LocalProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ReplicationProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.YaoABYProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ZKPProtocolParser
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.protocols.ArithABY
import edu.cornell.cs.apl.viaduct.protocols.BoolABY
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.protocols.YaoABY
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
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

    private val protocols: Map<ProtocolName, ProtocolParser<Protocol>> =
        mapOf(
            Local.protocolName to LocalProtocolParser,
            Commitment.protocolName to CommitmentProtocolParser,
            Replication.protocolName to ReplicationProtocolParser,
            ArithABY.protocolName to ArithABYProtocolParser,
            BoolABY.protocolName to BoolABYProtocolParser,
            YaoABY.protocolName to YaoABYProtocolParser,
            ZKP.protocolName to ZKPProtocolParser
        )

    private fun getProtocolBackends(): List<ProtocolBackend> {
        return listOf(
            PlaintextProtocolInterpreter,
            ABYProtocolInterpreter,
            CommitmentProtocolInterpreterFactory,
            ZKPProtocolInterpreterFactory
        )
    }

    override fun run() {
        val program = input.parse(protocols).elaborated()

        val connectionInfoMap: Map<Host, HostAddress> =
            hostAddress.map { kv ->
                val host = Host(kv.first)
                val addressStr = kv.second.split(":", limit = 2)
                val address = HostAddress(addressStr[0], addressStr[1].toInt())
                host to address
            }.toMap()

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
