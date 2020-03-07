package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import java.io.File

class Specification : CliktCommand(help = "Generate UC ideal functionality from source program") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    override fun run() {
        // Parse
        val program = input.parse().elaborated()

        // Check
        program.check()

        // Generate specification
        // TODO: Generate specification
    }
}
