package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import edu.cornell.cs.apl.viaduct.backend.PlaintextProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreterFactory
import edu.cornell.cs.apl.viaduct.backend.ViaductBackend
import edu.cornell.cs.apl.viaduct.backend.aby.ABYProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.commitment.CommitmentProtocolInterpreterFactory
import edu.cornell.cs.apl.viaduct.backend.zkp.ZKPProtocolInterpreterFactory
import edu.cornell.cs.apl.viaduct.parsing.AbyProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.CommitmentProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.LocalProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ReplicationProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ZKPProtocolParser
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import java.io.File
import kotlin.system.exitProcess
import org.apache.logging.log4j.core.config.Configurator

class Run : CliktCommand(help = "Run compiled protocol for a single host") {
    private val hostName by
    argument(
        "HOSTNAME",
        help = "Host that will run the protocol."
    )

    val input: File? by inputProgram()

    val verbose: Boolean by option(
        "-v",
        "--verbose",
        help = "Output logging information generated during execution"
    ).flag(default = false)

    private val protocols: Map<ProtocolName, ProtocolParser<Protocol>> =
        mapOf(
            Local.protocolName to LocalProtocolParser,
            Commitment.protocolName to CommitmentProtocolParser,
            Replication.protocolName to ReplicationProtocolParser,
            ZKP.protocolName to ZKPProtocolParser,
            ABY.protocolName to AbyProtocolParser
        )

    private fun getBackends(): Map<ProtocolName, ProtocolInterpreterFactory> {
        return mapOf(
            Local.protocolName to PlaintextProtocolInterpreter,
            Replication.protocolName to PlaintextProtocolInterpreter,
            ABY.protocolName to ABYProtocolInterpreter,
            Commitment.protocolName to CommitmentProtocolInterpreterFactory,
            ZKP.protocolName to ZKPProtocolInterpreterFactory
        )
    }

    override fun run() {
        if (verbose) {
            Configurator.setRootLevel(org.apache.logging.log4j.Level.INFO)
        }

        val program = input.parse(protocols).elaborated()
        val protocolBackends: Map<ProtocolName, ProtocolInterpreterFactory> = getBackends()
        val backend = ViaductBackend(protocolBackends)

        // interpret program
        backend.run(program, Host(hostName))
        exitProcess(0)
    }
}
