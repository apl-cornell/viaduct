package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import edu.cornell.cs.apl.viaduct.parsing.parseLabel
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specification
import java.io.File

class Specification : CliktCommand(help = "Generate UC ideal functionality from source program") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    val adversary: String by option(
        "-a",
        "--adversary",
        help = "Security label capturing the capabilities of the adversary"
    ).required()

    override fun run() {
        // Parse
        val program = input.parse().elaborated()
        val adversaryLabel = adversary.parseLabel(path = "<argument>")

        program.check()

        // Generate specification
        val specificationProgram = program.specification(adversaryLabel)

        output.print(specificationProgram)
    }
}
