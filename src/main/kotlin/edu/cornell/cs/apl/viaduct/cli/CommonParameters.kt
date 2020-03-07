package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.arguments.ArgumentDelegate
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.OptionDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import java.io.File
import java.io.IOException
import java.io.PrintStream
import org.fusesource.jansi.AnsiConsole
import org.fusesource.jansi.AnsiPrintStream

/** Adds an input parameter to a [CliktCommand]. */
internal fun CliktCommand.inputProgram(): ArgumentDelegate<File?> =
    argument(
        "FILE",
        help = "Read input program from <file> (default: stdin)"
    ).file(mustExist = true, canBeDir = false, mustBeReadable = true).optional()

/** Adds an output parameter to a [CliktCommand]. */
internal fun ParameterHolder.outputFile(): OptionDelegate<File?> =
    option(
        "-o",
        "--output",
        help = "Write output to FILE (default: stdout)"
    ).file(canBeDir = false, mustBeWritable = true)

/**
 * Parses the contents of [this] file as a program. If [this] is `null`, the standard input is
 * parsed instead.
 *
 * @throws IOException
 */
internal fun File?.parse(): ProgramNode =
    (if (this == null)
        System.`in`.bufferedReader().use { SourceFile.from("<stdin>", it) }
    else
        SourceFile.from(this)).parse()

/**
 * Pretty prints [document] to [this] file. If [this] is `null`, [document] is printed to the
 * standard output instead.
 *
 * @throws IOException
 */
internal fun File?.print(document: PrettyPrintable) {
    this.output { document.asDocument.print(it, ansi = true) }
}

/**
 * Opens this [File] and executes the given [block] function on it. Then, closes the file down
 * correctly whether an exception is thrown or not. If the file is `null`, [block] is run on the
 * standard output, which is not closed.
 */
private fun <R> File?.output(block: (PrintStream) -> R): R {
    // TODO: PrintStream doesn't throw errors when writing. These will fail silently.
    return if (this == null) {
        // Write to standard out.
        block(AnsiConsole.out)
    } else {
        // Write to the given file, but strip out ANSI escape codes.
        AnsiPrintStream(PrintStream(this, Charsets.UTF_8)).use(block)
    }
}
