package io.github.apl_cornell.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.arguments.ArgumentDelegate
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.OptionDelegate
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.squareup.kotlinpoet.FileSpec
import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.viaduct.parsing.SourceFile
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.fusesource.jansi.AnsiConsole
import java.io.File
import java.io.IOException
import java.io.PrintStream

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
    ).file(canBeDir = false, mustExist = false)

/** Adds a command line option that sets the application logging level. */
internal fun ParameterHolder.verbosity(): OptionDelegate<Int> =
    option(
        "-v",
        "--verbose",
        help = """
            Print debugging information

            Repeat for more and more granular messages.
        """
    ).counted().validate {
        // Set the global logging level.
        // Note: this is not how `.validate` is meant to be used, but it's the closest feature Clikt provides.

        val level = when (it) {
            0 -> null
            1 -> Level.INFO
            2 -> Level.DEBUG
            3 -> Level.TRACE
            else -> Level.ALL
        }

        if (level != null) Configurator.setRootLevel(level)
    }

/** Returns a [SourceFile] constructed from [this]. Uses the standard input if [this] is `null`. */
internal fun File?.sourceFile(): SourceFile =
    if (this == null)
        System.`in`.bufferedReader().use { SourceFile.from("<stdin>", it) }
    else
        SourceFile.from(this)

/**
 * Jansi has this annoying behavior of reading [java.io.FileDescriptor.out] directly instead of using [System.out].
 * This causes a bunch of stuff to be printed during testing, because JUnit replaces the [System] streams, but it
 * cannot replace the file descriptors. Moreover, JUnit cannot capture testing output when it is directly printed to
 * file descriptors.
 *
 * Setting this flag makes all output sidestep [AnsiConsole].
 */
internal var testing: Boolean = false

/**
 * Pretty prints [document] (plus the line separator) to [this] file. If [this] is `null`, [document] is printed to the
 * standard output instead.
 *
 * @throws IOException
 */
internal fun File?.println(document: PrettyPrintable) {
    val doc = document.toDocument() + Document.lineBreak
    if (this == null) {
        // Write to standard out.
        doc.print(if (testing) System.out else AnsiConsole.out(), ansi = true)
    } else {
        // Write to the given file, but exclude ANSI escape codes.
        PrintStream(this, Charsets.UTF_8).use { doc.print(it, ansi = false) }
    }
}

/** Writes [program] to [this] file. Writes to the standard output if [this] is `null`. */
internal fun File?.write(program: FileSpec) =
    if (this != null)
        writer().use { program.writeTo(it) }
    else
        program.writeTo(System.out)
