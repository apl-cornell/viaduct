package io.github.aplcornell.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.aplcornell.viaduct.backends.DefaultCombinedBackend
import io.github.aplcornell.viaduct.parsing.parse
import io.github.aplcornell.viaduct.passes.check
import io.github.aplcornell.viaduct.passes.elaborated
import io.github.aplcornell.viaduct.passes.specialize
import java.io.File

class Format : CliktCommand(help = "Pretty print source program") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    val elaborate by option("-e", "--elaborate", help = "Print internal representation").flag()

    val check by option("-c", "--check", help = "Type check the program before printing").flag()

    override fun run() {
        val program = input.sourceFile().parse(DefaultCombinedBackend.protocolParsers)
        val elaborated by lazy { program.elaborated() }

        if (check) {
            elaborated.check()
        }

        output.println(if (elaborate) elaborated.specialize() else program)
    }
}
