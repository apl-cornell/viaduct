package io.github.apl_cornell.viaduct.parsing

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.Style
import io.github.apl_cornell.viaduct.prettyprinting.concatenated
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.prettyprinting.styled
import io.github.apl_cornell.viaduct.prettyprinting.times

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
        // List of lines to be printed
        val relevantLines: IntRange = run {
            val firstLine = (start.line - contextLines).coerceAtLeast(1)
            val lastLine = (end.line + contextLines).coerceAtMost(sourceFile.numberOfLines)
            // TODO: trim blank lines?
            firstLine..lastLine
        }

        // Number of characters it takes to represent the largest line number.
        val lineNumberWidth = relevantLines.last.toString().length

        // True if we are highlighting multiple lines; false otherwise.
        val highlightingMultipleLines = start.line != end.line

        // Print relevant lines
        val outputLines: List<Document> = relevantLines.map { line ->
            val thisLineShouldBeHighlighted = line in start.line..end.line

            val lineNumber = String.format("%${lineNumberWidth}d|", line)

            // In multiline mode, we put a marker next to the line number to indicate the entire line is relevant.
            val multilineMarker =
                when {
                    highlightingMultipleLines && thisLineShouldBeHighlighted ->
                        Document(">").styled(highlightStyle)
                    highlightingMultipleLines ->
                        Document(" ")
                    else ->
                        Document()
                }

            // In single-line mode, we underline the relevant portion.
            val underline =
                if (!highlightingMultipleLines && thisLineShouldBeHighlighted) {
                    val highlightStartColumn = lineNumber.length + 1 + start.column
                    val highlightLength = end.column - start.column
                    Document.forcedLineBreak +
                        " ".repeat(highlightStartColumn - 1) +
                        Document("^".repeat(highlightLength)).styled(highlightStyle)
                } else Document()

            Document(lineNumber) + multilineMarker * Document(sourceFile.getLine(line)) + underline
        }

        // Make sure there is uniform vertical spacing after the displayed source code.
        // Note that we consider a line blank if it only contains characters used to underline.
        val bottomPadding =
            if (highlightingMultipleLines || end.line != relevantLines.last) {
                // Last line did not have an underline. Add blank line instead.
                Document.forcedLineBreak
            } else
                Document()

        return outputLines.concatenated(Document.forcedLineBreak) + bottomPadding
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
