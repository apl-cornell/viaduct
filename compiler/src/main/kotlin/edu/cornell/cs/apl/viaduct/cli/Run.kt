package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.backend.PlaintextProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreterFactory
import edu.cornell.cs.apl.viaduct.backend.ViaductBackend
import edu.cornell.cs.apl.viaduct.backend.aby.ABYProtocolInterpreter
import edu.cornell.cs.apl.viaduct.parsing.AbyProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.LocalProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ReplicationProtocolParser
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import java.io.File

class Run : CliktCommand(help = "Run compiled protocol for a single host") {
    private val hostName by
    argument(
        "HOSTNAME",
        help = "Host that will run the protocol."
    )

    val input: File? by inputProgram()

    val protocolSelectionOutput: File? by option(
        "-s",
        "--selection",
        metavar = "FILE.via",
        help = "Write program decorated with protocol selection to FILE.via"
    ).file(canBeDir = false)

    private fun dumpProgramMetadata(
        program: ProgramNode,
        metadata: Map<Node, PrettyPrintable>,
        file: File?
    ) {
        if (file == null) {
            return
        }

        file.println(program.printMetadata(metadata))
    }

    private val protocols: Map<ProtocolName, ProtocolParser<Protocol>> =
        mapOf(
            Local.protocolName to LocalProtocolParser,
            Replication.protocolName to ReplicationProtocolParser,
            ABY.protocolName to AbyProtocolParser
        )

    private fun getBackends(): Map<ProtocolName, ProtocolInterpreterFactory> {
        return mapOf(
            Local.protocolName to PlaintextProtocolInterpreter,
            Replication.protocolName to PlaintextProtocolInterpreter,
            ABY.protocolName to ABYProtocolInterpreter
        )
    }

    override fun run() {
        val unspecializedProgram = input.parse(protocols).elaborated()
        val program = unspecializedProgram.specialize()

        // Perform static checks.
        program.check()

        val protocolFactory = SimpleProtocolFactory(program)

        // Select protocols.
        val protocolAssignment: (FunctionName, Variable) -> Protocol =
            selectProtocolsWithZ3(
                program,
                program.main,
                protocolFactory,
                SimpleCostEstimator
            ) { metadata -> dumpProgramMetadata(program, metadata, protocolSelectionOutput) }

        println("compiled protocol selection")

        val protocolAnalysis = ProtocolAnalysis(program, protocolAssignment, SimpleProtocolComposer)

        val protocolBackends: Map<ProtocolName, ProtocolInterpreterFactory> = getBackends()
        val backend = ViaductBackend(protocolBackends)

        val host = Host(hostName)

        // TODO: Post-process program; handle muxing

        // interpret program
        backend.run(program, protocolAnalysis, host)
    }
}
