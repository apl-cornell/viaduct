package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.BackendInterpreter
import edu.cornell.cs.apl.viaduct.backend.PlaintextBackend
import edu.cornell.cs.apl.viaduct.backend.aby.ABYBackend
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.protocols.ABYFactory
import edu.cornell.cs.apl.viaduct.protocols.LocalFactory
import edu.cornell.cs.apl.viaduct.protocols.ReplicationFactory
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ProtocolParser
import java.io.File

class Run : CliktCommand(help = "Run compiled protocol for a single host") {
    private val hostName by
    argument(
        "HOSTNAME",
        help = "Host that will run the protocol."
    )

    val input: File? by inputProgram()

    private fun registerProtocols(protocolParser: ProtocolParser) {
        protocolParser.registerProtocolFactory(LocalFactory())
        protocolParser.registerProtocolFactory(ReplicationFactory())
        protocolParser.registerProtocolFactory(ABYFactory())
        ABYBackend()
    }

    private fun registerBackends(interpreter: BackendInterpreter) {
        interpreter.registerBackend(PlaintextBackend())
        interpreter.registerBackend(ABYBackend())
    }

    override fun run() {
        val protocolParser = ProtocolParser()
        registerProtocols(protocolParser)

        val program = input.parse().elaborated(protocolParser)
        val nameAnalysis = NameAnalysis(Tree(program))
        val typeAnalysis = TypeAnalysis(nameAnalysis)

        val interpreter = BackendInterpreter(nameAnalysis, typeAnalysis)
        registerBackends(interpreter)

        val host = Host(hostName)
        interpreter.run(program, host)
    }
}
