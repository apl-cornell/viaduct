package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.AnsiBaseColor
import io.github.aplcornell.viaduct.prettyprinting.AnsiColor
import io.github.aplcornell.viaduct.prettyprinting.DEFAULT_LINE_WIDTH
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.NormalColor
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.prettyprinting.Style
import io.github.aplcornell.viaduct.prettyprinting.div
import io.github.aplcornell.viaduct.prettyprinting.nested
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.styled
import io.github.aplcornell.viaduct.syntax.SourceLocation

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

    /** Displays [this] message followed by the portion of the source code indicated by [sourceLocation]. */
    protected fun Document.withSource(sourceLocation: SourceLocation): Document =
        this / (Document.lineBreak + sourceLocation.showInSource(SourceHighlightingStyle))

    /** Displays [this] message followed by [body] with [body] on its own line. */
    protected fun Document.withData(body: PrettyPrintable): Document =
        this / (Document.lineBreak + body).nested() + Document.lineBreak

    final override fun toDocument(): Document {
        val header = run {
            val title = "-- ${category.uppercase()} -"
            val source = " " + this.source
            val paddingLength = (DEFAULT_LINE_WIDTH - title.length - source.length).coerceAtLeast(0)
            val padding = "-".repeat(paddingLength)
            (Document(title) + padding + source).styled(HeaderStyle)
        }

        val hint = hint?.let { Document.lineBreak + it } ?: Document()

        return header + Document.lineBreak + Document.lineBreak + description + hint
    }

    final override fun toString(): String =
        this.toDocument().print()
}
