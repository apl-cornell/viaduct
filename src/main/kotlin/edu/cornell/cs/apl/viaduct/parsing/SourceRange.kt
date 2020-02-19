package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.styled
import edu.cornell.cs.apl.prettyprinting.times

/**
 * Denotes a nonempty set of consecutive characters in a source file. Empty ranges are not allowed,
 * since there is no column number that corresponds to a single position.
 *
 * @param start position that comes just before the characters in the range
 * @param end position that comes just after the characters in the range
 */
data class SourceRange(val start: SourcePosition, val end: SourcePosition) {
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
     * Displays the portions of the source file containing this location and highlights the region
     * corresponding to this location.
     *
     * @param highlightStyle [Style] to use for highlighting.
     * @param contextLines Number of lines before and after the relevant region to display to give
     *     more context to the user.
     */
    fun showInSource(
        highlightStyle: Style,
        contextLines: Int = if (start.line == end.line) 0 else 1
    ): Document {
        // Number of characters it takes to represent the largest line number.
        val lineNumberWidth = end.line.toString().length

        // True if we are highlighting multiple lines; false otherwise.
        val multilineHighlight = start.line != end.line

        // Print relevant lines
        val firstLine = (start.line - contextLines).coerceAtLeast(1)
        val lastLine = (end.line + contextLines).coerceAtMost(sourceFile.numberOfLines)
        var output: Document = Document()
        for (line in firstLine..lastLine) {
            val lineNumber = String.format("%${lineNumberWidth}d|", line)
            val highlightThisLine = line in start.line..end.line

            // Print line number
            output += lineNumber

            // In multiline mode, mark the entire line as relevant with an indicator
            if (multilineHighlight) {
                output +=
                    if (highlightThisLine)
                        Document(">").styled(highlightStyle)
                    else Document(" ")
            }

            // Print the actual line with a space between line numbers and line contents
            output *= sourceFile.getLine(line) + Document.forcedLineBreak

            // In single-line mode, underline the relevant portion
            if (!multilineHighlight && highlightThisLine) {
                val highlightStartColumn = lineNumber.length + 1 + start.column
                val highlightLength = end.column - start.column
                output += " ".repeat(highlightStartColumn - 1)
                output += Document("^".repeat(highlightLength)).styled(highlightStyle)
                output += Document.forcedLineBreak
            }
        }

        // Make sure there is uniform vertical space after the displayed source code.
        if (multilineHighlight || contextLines > 0) {
            // Last line did not have an underline. Add blank line instead.
            output += Document.forcedLineBreak
        }

        return output
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
