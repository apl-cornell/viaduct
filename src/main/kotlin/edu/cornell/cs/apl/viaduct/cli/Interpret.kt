package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import java.io.File

class Interpret : CliktCommand(help = "Execute program and print its final state") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    override fun run() {
        // Parse
        val program = input.parse().elaborated()

        // Check
        program.check()

        // Interpret
        // TODO: interpret and print result
    }
}
