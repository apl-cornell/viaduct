package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.viaduct.util.PrintUtil
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiPrintStream
import java.io.PrintStream

/**
 * Superclass of all errors caused by bad user input to the compiler.
 *
 * Errors caused by bugs in the compiler do not belong here.
 * They should instead raise a (subclass of) [RuntimeException].
 */
abstract class CompilationError : Error() {
    /** General description (i.e. title) of the error. */
    protected abstract val category: String

    /** Name of the file or description of the source that caused the error. */
    protected abstract val source: String

    /**
     * Print colored error description to the given stream.
     *
     * The output stream should be ready to handle ANSI color codes. If the output stream does not
     * support color codes, they can be stripped by wrapping the output stream in [AnsiPrintStream].
     */
    open fun print(output: PrintStream) {
        // Print title line
        val title = "-- ${category.toUpperCase()} -"
        val paddingLength = PrintUtil.LINE_WIDTH - source.length - title.length - 1
        val padding = "-".repeat(paddingLength.coerceAtLeast(0))
        output.println(Ansi.ansi().fg(Ansi.Color.CYAN).a(title).a(padding).a(' ').a(source).reset())
        output.println()
    }

    /** Add the default amount of indentation to the output.  */
    protected fun addIndentation(output: PrintStream) {
        output.print(" ".repeat(PrintUtil.INDENTATION_LEVEL))
    }

    final override fun toString(): String {
        return PrintUtil.printToString { output: PrintStream -> this.print(output) }
    }
}
