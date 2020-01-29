package edu.cornell.cs.apl.viaduct.parser

import org.fusesource.jansi.Ansi
import java.io.PrintStream

/**
 * Denotes a nonempty set of consecutive characters in a source file. Empty ranges are not allowed,
 * since there is no column number that corresponds to a single position.
 *
 * @param start position that comes just before the characters in the range
 * @param end position that comes just after the characters in the range
 */
data class SourceRange(private val start: SourcePosition, private val end: SourcePosition) {
    init {
        require(start.sourceFile == end.sourceFile) { "Positions must be in the same file." }
        require(start != end) { "Source range cannot be empty." }
        require(start.offset <= end.offset) { "Start position cannot come after the end position." }
    }

    /** Description of where the source file came from.  */
    val sourcePath: String
        get() = sourceFile.path

    private val sourceFile: SourceFile
        get() = start.sourceFile

    /**
     * Combines two ranges to create a range that spans both.
     *
     * @return The merged range.
     */
    fun merge(that: SourceRange): SourceRange {
        val start = minOf(this.start, that.start)
        val end = maxOf(this.end, that.end)
        return SourceRange(start, end)
    }

    /**
     * Prints the relevant portions of the source file and highlights the region that corresponds to
     * this location.
     *
     * @param output where to print the output
     * @param contextLines number of lines before and after the relevant region to display to give
     * more context to the user
     */
    fun showInSource(
        output: PrintStream,
        contextLines: Int = if (start.line == end.line) 0 else 1
    ) {
        // Number of characters it takes to represent the largest line number.
        val lineNumberWidth = end.line.toString().length

        // True if we are highlighting multiple lines; false otherwise.
        val multiLineMode = start.line != end.line

        // Print relevant lines
        val firstLine = (start.line - contextLines).coerceAtLeast(1)
        val lastLine = (end.line + contextLines).coerceAtMost(sourceFile.numberOfLines)
        for (line in firstLine..lastLine) {
            val lineNumber = String.format("%${lineNumberWidth}d|", line)
            val highlightThisLine = line in start.line..end.line

            // Print line number
            output.print(lineNumber)

            // In multiline mode, mark the entire line as relevant with an indicator
            if (multiLineMode) {
                if (highlightThisLine) {
                    output.print(Ansi.ansi().fg(Ansi.Color.RED).a('>').reset())
                } else {
                    output.print(" ")
                }
            }

            // Print space between line numbers and line contents
            output.print(" ")

            // Print the actual line
            output.println(sourceFile.getLine(line))

            // In single-line mode, underline the relevant portion
            if (!multiLineMode && highlightThisLine) {
                val highlightStartColumn = lineNumber.length + 1 + start.column
                val highlightLength = end.column - start.column
                output.print(" ".repeat(highlightStartColumn - 1))
                output.println(
                    Ansi.ansi().fg(Ansi.Color.RED).a("^".repeat(highlightLength)).reset()
                )
            }
        }

        // Make sure there is uniform vertical space after the displayed source code.
        if (multiLineMode || contextLines > 0) {
            // Last line did not have an underline. Add blank line instead.
            output.println()
        }
    }

    override fun toString(): String {
        val startColumn = start.column
        val endColumn = end.column - 1
        return if (start.line == end.line) {
            "$sourcePath:${start.line}:$startColumn-$endColumn"
        } else {
            val start = "(${start.line}:$startColumn)"
            val end = "(${end.line}:$endColumn)"
            "$sourcePath:$start-$end"
        }
    }
}
