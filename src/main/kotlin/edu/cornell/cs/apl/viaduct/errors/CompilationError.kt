package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.AnsiBaseColor
import edu.cornell.cs.apl.prettyprinting.AnsiColor
import edu.cornell.cs.apl.prettyprinting.DEFAULT_LINE_WIDTH
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.NormalColor
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.div
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.styled
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation

/**
 * Superclass of all errors caused by bad user input to the compiler.
 *
 * Errors caused by bugs in the compiler do not belong here.
 * They should instead raise a (subclass of) [RuntimeException].
 */
abstract class CompilationError : Error(), PrettyPrintable {
    /** General description (i.e. title) of the error. */
    protected abstract val category: String

    /** Name of the file or description of the source that contains the error. */
    protected abstract val source: String

    /** Detailed description of the error. */
    protected abstract val description: Document

    /** A hint that can help resolve the error. */
    protected open val hint: Document?
        get() = null

    private object HeaderStyle : Style {
        override val foregroundColor: AnsiColor
            get() = NormalColor(AnsiBaseColor.CYAN)
    }

    private object SourceHighlightingStyle : Style {
        override val foregroundColor: AnsiColor
            get() = NormalColor(AnsiBaseColor.RED)
    }

    /**
     * Displays this message followed by the portion of the source code indicated
     * by [sourceLocation].
     */
    protected fun Document.withSource(sourceLocation: SourceLocation): Document =
        this / (Document.lineBreak + sourceLocation.showInSource(SourceHighlightingStyle))

    /** Displays this message followed by [body] with [body] on its own line. */
    protected fun Document.withData(body: PrettyPrintable): Document =
        this / (Document.lineBreak + body).nested() + Document.lineBreak + Document.lineBreak

    final override val asDocument: Document
        get() {
            val title = "-- ${category.toUpperCase()} -"
            val source = " " + this.source
            val paddingLength = (DEFAULT_LINE_WIDTH - title.length - source.length).coerceAtLeast(0)
            val padding = "-".repeat(paddingLength)
            val header = (Document(title) + padding + source).styled(HeaderStyle)

            val hint = hint?.plus(Document.lineBreak + Document.lineBreak) ?: Document()

            return header + Document.lineBreak + Document.lineBreak + description + hint
        }

    final override fun toString(): String =
        this.asDocument.print()
}
