package edu.cornell.cs.apl.viaduct

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.cli.Viaduct
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import java.io.IOException
import kotlin.system.exitProcess
import org.fusesource.jansi.AnsiConsole

/** Runs the compiler.  */
fun main(args: Array<String>) {
    try {
        Viaduct().main(args)
    } catch (e: Throwable) {
        failWith(e)
    }
}

/**
 * Prints a useful error message based on the exception [e]. Then terminate with a non-zero exit
 * code.
 */
private fun failWith(e: Throwable) {
    when (e) {
        is IOException -> {
            AnsiConsole.err().println(e.getLocalizedMessage())
        }
        is CompilationError -> {
            // User error. Print short, pretty message.
            (e.asDocument + Document.lineBreak).print(AnsiConsole.err(), ansi = true)
        }
        else -> {
            // Developer error. Give more detail.
            e.printStackTrace()
            exitProcess(2)
        }
    }
    exitProcess(1)
}
