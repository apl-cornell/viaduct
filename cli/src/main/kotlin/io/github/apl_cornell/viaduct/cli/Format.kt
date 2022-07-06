package io.github.apl_cornell.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.parsing.parse
import io.github.apl_cornell.viaduct.passes.check
import io.github.apl_cornell.viaduct.passes.elaborated
import io.github.apl_cornell.viaduct.passes.specialize
import java.io.File

class Format : CliktCommand(help = "Pretty print source program") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    val elaborate by option("-e", "--elaborate", help = "Print internal representation").flag()

    val check by option("-c", "--check", help = "Type check the program before printing").flag()

    override fun run() {
        val program = input.sourceFile().parse(DefaultCombinedBackend.protocolParsers)
        val elaborated by lazy { program.elaborated().specialize() }

        if (check) {
            elaborated.check()
        }

        output.println(if (elaborate) elaborated else program)
    }
}
