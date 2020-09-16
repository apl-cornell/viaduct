package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.specialize
import java.io.File

class Format : CliktCommand(help = "Pretty print source program") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    val elaborate by option("-e", "--elaborate", help = "Print internal representation").flag()

    val check by option("-c", "--check", help = "Type check the program before printing").flag()

    override fun run() {
        val program = input.parse()
        val elaborated by lazy { program.elaborated().specialize() }

        if (check) {
            elaborated.check()
        }

        output.println(if (elaborate) elaborated else program)
    }
}
