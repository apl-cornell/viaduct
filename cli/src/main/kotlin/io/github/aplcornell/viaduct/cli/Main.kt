package io.github.aplcornell.viaduct.cli

import io.github.aplcornell.viaduct.errors.CompilationError
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.plus
import org.fusesource.jansi.AnsiConsole
import java.io.IOException
import kotlin.system.exitProcess

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
            System.err.println(e.getLocalizedMessage())
        }
        is CompilationError -> {
            // User error. Print short, pretty message.
            (e.toDocument() + Document.lineBreak).print(AnsiConsole.err(), ansi = true)
        }
        else -> {
            // Developer error. Give more detail.
            e.printStackTrace()
            exitProcess(2)
        }
    }
    exitProcess(1)
}
