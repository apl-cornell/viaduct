package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import edu.cornell.cs.apl.viaduct.parsing.ArithABYProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.BoolABYProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.CommitmentProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.LocalProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.ReplicationProtocolParser
import edu.cornell.cs.apl.viaduct.parsing.YaoABYProtocolParser
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import edu.cornell.cs.apl.viaduct.protocols.ArithABY
import edu.cornell.cs.apl.viaduct.protocols.BoolABY
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.protocols.YaoABY
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import java.io.File

class Format : CliktCommand(help = "Pretty print source program") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    val elaborate by option("-e", "--elaborate", help = "Print internal representation").flag()

    val check by option("-c", "--check", help = "Type check the program before printing").flag()

    private val protocols: Map<ProtocolName, ProtocolParser<Protocol>> =
        mapOf(
            Local.protocolName to LocalProtocolParser,
            Commitment.protocolName to CommitmentProtocolParser,
            Replication.protocolName to ReplicationProtocolParser,
            ArithABY.protocolName to ArithABYProtocolParser,
            BoolABY.protocolName to BoolABYProtocolParser,
            YaoABY.protocolName to YaoABYProtocolParser
        )

    override fun run() {
        val program = input.parse(protocols)
        val elaborated by lazy { program.elaborated().specialize() }

        if (check) {
            elaborated.check()
        }

        output.println(if (elaborate) elaborated else program)
    }
}
