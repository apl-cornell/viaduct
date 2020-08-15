package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import edu.cornell.cs.apl.viaduct.backend.BackendInterpreter
import edu.cornell.cs.apl.viaduct.backend.PlaintextBackend
import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.aby.ABYBackend
import edu.cornell.cs.apl.viaduct.parsing.AbyProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.LocalProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ReplicationProtocolParser
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import java.io.File

class Run : CliktCommand(help = "Run compiled protocol for a single host") {
    private val hostName by
    argument(
        "HOSTNAME",
        help = "Host that will run the protocol."
    )

    val input: File? by inputProgram()

    private val protocols: Map<ProtocolName, ProtocolParser<Protocol>> =
        mapOf(
            Local.protocolName to LocalProtocolParser,
            Replication.protocolName to ReplicationProtocolParser,
            ABY.protocolName to AbyProtocolParser
        )

    private fun getBackends(): Map<ProtocolName, ProtocolBackend> {
        val plaintextBackend = PlaintextBackend()

        return mapOf(
            Local.protocolName to plaintextBackend,
            Replication.protocolName to plaintextBackend,
            ABY.protocolName to ABYBackend()
        )
    }

    override fun run() {
        val program = input.parse(protocols).elaborated()
        val backends: Map<ProtocolName, ProtocolBackend> = getBackends()
        val interpreter = BackendInterpreter(backends)

        val host = Host(hostName)
        interpreter.run(program, host)
    }
}
