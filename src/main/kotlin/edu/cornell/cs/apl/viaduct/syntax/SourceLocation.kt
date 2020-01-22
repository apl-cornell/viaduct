package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange
import java.io.PrintStream

/** Specifies what portion of the source code an abstract syntax tree node corresponds to. */
data class SourceLocation(private val location: SourceRange) {
    /** Description of where the source file came from. */
    val sourcePath: String
        get() = location.sourcePath

    private val singleLine
        get() = location.start.line == location.end.line

    /**
     * Print the relevant portions of the source file and highlight the region that corresponds to
     * this location.
     *
     * @param output where to print the output
     * @param contextLines number of lines before and after the relevant region to display to give
     *     more context to the user
     */
    fun showInSource(output: PrintStream, contextLines: Int = if (singleLine) 0 else 1) {
        location.showInSource(output, contextLines)
    }

    override fun toString(): String {
        return location.toString()
    }
}
