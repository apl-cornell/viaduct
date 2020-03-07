package edu.cornell.cs.apl.prettyprinting

import edu.cornell.cs.apl.prettyprinting.Document.Companion.invoke
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import org.fusesource.jansi.Ansi

/** The maximum number of characters to put on one line. */
const val DEFAULT_LINE_WIDTH = 80

/**
 * Represents a pretty printed document.
 *
 * More specifically, a [Document] represents a non-empty set of possible layouts of a document.
 * The [print] functions select one of these possibilities, taking into account things like the
 * width of the output document.
 *
 * [Document]s are created using the [invoke] functions, but the main interface is through
 * [PrettyPrintable].
 */
sealed class Document : PrettyPrintable {
    /** Returns this object. */
    override val asDocument: Document
        get() = this

    /** Do not use [toString]; use [print] instead. */
    final override fun toString(): String {
        throw UnsupportedOperationException("Do not use Document.toString(). Use print instead.")
    }

    /**
     * Renders this document as a string and writes it to [output]. Each line of the output
     * will be under [lineWidth] characters if possible. If [ansi] is `true`, ANSI escape codes
     * are output to style the document according to the [Style] annotations in the document.
     */
    // TODO: respect [lineWidth]
    fun print(output: PrintStream, lineWidth: Int = DEFAULT_LINE_WIDTH, ansi: Boolean = false) {
        require(lineWidth > 0)

        /**
         * Recursively prints the document. Does the bulk of the work.
         *
         * @param indentation current indentation level
         * @param style current style
         */
        fun Document.traverse(indentation: Int, style: Style) {
            when (this) {
                is Empty ->
                    Unit

                is Text -> {
                    if (ansi)
                        output.print(style.toAnsi().a(this.text).reset())
                    else
                        output.print(this.text)
                }

                is LineBreak -> {
                    output.println()
                    output.print(" ".repeat(indentation))
                }

                is SoftLineBreak -> {
                    output.println()
                    output.print(" ".repeat(indentation))
                }

                is Concatenated -> {
                    this.document1.traverse(indentation, style)
                    this.document2.traverse(indentation, style)
                }

                is Nested -> {
                    this.document.traverse(indentation + this.indentationChange, style)
                }

                is Styled -> {
                    this.document.traverse(indentation, this.style)
                }
            }
        }

        if (ansi)
            output.print(Ansi().reset())
        this.traverse(0, DefaultStyle)
        output.flush()
    }

    /**
     * Renders the document as a [String].
     *
     * @see [print]
     */
    fun print(lineWidth: Int = DEFAULT_LINE_WIDTH): String {
        val byteStream = ByteArrayOutputStream()
        PrintStream(byteStream, false, Charset.defaultCharset()).use {
            this.print(it, lineWidth, ansi = false)
            return String(byteStream.toByteArray(), Charset.defaultCharset())
        }
    }

    /** Exposed as [PrettyPrintable.nested]. */
    internal fun nested(indentationChange: Int): Document =
        Nested(this, indentationChange)

    /** Exposed as [PrettyPrintable.grouped]. */
    internal fun grouped(): Document {
        // TODO: simply replacing all soft line breaks with spaces for now.
        //  Do proper layout in the future.
        return when (this) {
            is Empty ->
                this

            is Text ->
                this

            is Concatenated ->
                Document(document1.grouped(), document2.grouped())

            is LineBreak ->
                this

            is SoftLineBreak ->
                Text(" ")

            is Nested ->
                this.copy(document = document.grouped())

            is Styled ->
                this.copy(document = document.grouped())
        }
    }

    /** Exposed as [PrettyPrintable.styled]. */
    internal fun styled(style: Style): Document =
        Styled(this, style)

    companion object {
        /**
         * Returns the empty document. The empty document behaves the same as `Document("")`,
         * which means it has a height of 1, not 0. For example,
         * ```
         * Document("hello") / Document() / Document("world")
         * ```
         * produces
         * ```
         * hello
         *
         * world
         * ```
         *
         * The empty document is the left and right unit to [PrettyPrintable.plus]. That is
         * ```
         * Document() + Document("hello") == Document("hello") == Document("hello") + Document()
         * ```
         */
        operator fun invoke(): Document {
            return Empty
        }

        /**
         * Returns the document containing [text] converting all line breaks to [lineBreak].
         * Note that [lineBreak] may be undone by [PrettyPrintable.grouped]:
         *
         * ```
         * >>> Document("hello\nworld")
         * hello
         * world
         *
         * >>> Document("hello\nworld").grouped()
         * hello world
         * ```
         */
        @JvmStatic
        operator fun invoke(text: String): Document {
            val lines = text.split(unicodeLineBreak)
            val documents = lines.map { if (it.isEmpty()) Empty else Text(it) }
            return documents.concatenated(separator = lineBreak)
        }

        /** Exposed as [PrettyPrintable.plus]. */
        internal operator fun invoke(document1: Document, document2: Document): Document {
            return when {
                document1 is Empty ->
                    document2
                document2 is Empty ->
                    document1
                else ->
                    Concatenated(document1, document2)
            }
        }

        /**
         * A line break advances to the next line and indents to the current nesting level.
         *
         * ```
         * >>> Document("hello") + lineBreak + "world"
         * hello
         * world
         * ```
         *
         * A line break may be converted into a spaces if the document is [PrettyPrintable.grouped]
         * and there is enough space.
         *
         * ```
         * >>> (Document("hello") + lineBreak + "world").grouped()
         * hello world
         * ```
         */
        val lineBreak: Document = SoftLineBreak

        /**
         * Like [lineBreak] but always starts a new line.
         * A [forcedLineBreak] is never removed or replaced, even when [PrettyPrintable.grouped]
         * and there is plenty of space.
         *
         * ```
         * >>> Document("hello") + forcedLineBreak + "world"
         * hello
         * world
         * ```
         *
         * ```
         * >>> (Document("hello") + forcedLineBreak + "world").grouped()
         * hello
         * world
         * ```
         */
        val forcedLineBreak: Document = LineBreak
    }
}

/** The empty document. */
private object Empty : Document()

/** A document representing a nonempty single-line string. */
private data class Text(val text: String) : Document() {
    init {
        require(text.isNotEmpty())
        require(!text.contains(unicodeLineBreak))
    }
}

/** The concatenation of two documents. */
private data class Concatenated(val document1: Document, val document2: Document) : Document() {
    init {
        require(document1 !is Empty)
        require(document2 !is Empty)
    }
}

/** A forced line break. */
private object LineBreak : Document()

/** A line break that can be removed by [PrettyPrintable.grouped]. */
// TODO  We will probably remove this in the future and add something more general.
private object SoftLineBreak : Document()

/** Same as [document] but with its indentation level adjusted by [indentationChange]. */
private data class Nested(val document: Document, val indentationChange: Int) : Document()

/** Same as [document] but with [style] applied. */
private data class Styled(val document: Document, val style: Style) : Document()

/** A regular expression that recognizes Unicode line breaks. */
private val unicodeLineBreak: Regex = Regex("\\R")
