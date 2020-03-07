package edu.cornell.cs.apl.prettyprinting

import edu.cornell.cs.apl.prettyprinting.Document.Companion.invoke
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import org.fusesource.jansi.Ansi

/** The maximum number of characters to put on one line. */
const val DEFAULT_LINE_WIDTH = 80

/** An object that has a pretty text representation. */
interface PrettyPrintable {
    /** The pretty text representation of this object. */
    val asDocument: Document
}

/**
 * Concatenates [this] and [other].
 *
 * ```
 * >>> Document("hello") + "World"
 * helloWorld
 * ```
 */
operator fun PrettyPrintable.plus(other: PrettyPrintable): Document {
    val document1 = this.asDocument
    val document2 = other.asDocument
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
 * Concatenates [this] and [other] with a space in between.
 *
 * Equivalent to `this + " " + other`.
 *
 * ```
 * >>> Document("hello") * "world"
 * hello world
 * ```
 */
operator fun PrettyPrintable.times(other: PrettyPrintable): Document =
    this + " " + other

/**
 * Concatenates [this] and [other] with a line break in between.
 *
 * Equivalent to `this + Document.lineBreak + other`.
 *
 * ```
 * >>> Document("hello") / "world"
 * hello
 * world
 * ```
 */
operator fun PrettyPrintable.div(other: PrettyPrintable): Document =
    this + Document.lineBreak + other

/**
 * Convenience method that automatically converts [other] to a [Document].
 *
 * Allows the use of [String] literals when generating [Document]s. For example,
 * ```
 * Document("hello") + Document("world")
 * ```
 * can instead be written as
 * ```
 * Document("hello") + "world"
 * ```
 *
 * Unfortunately, the first call to [Document.invoke] cannot be avoided.
 *
 * @see [plus]
 */
operator fun PrettyPrintable.plus(other: String): Document =
    this + Document(other)

/** Convenience method. See [plus]. */
operator fun PrettyPrintable.times(other: String): Document =
    this * Document(other)

/** Convenience method. See [plus]. */
operator fun PrettyPrintable.div(other: String): Document =
    this / Document(other)

/**
 * Concatenates all the elements together with [separator]s in between.
 *
 * ```
 * val docs = listOf("lorem", "ipsum", "dolor", "sit").map { Document(it) }
 * >>> docs.concatenated(separator = Document(", "))
 * lorem, ipsum, dolor, sit
 * ```
 */
fun List<PrettyPrintable>.concatenated(separator: PrettyPrintable = Document()): Document {
    return if (this.isEmpty())
        Document()
    else reduce { acc, next -> acc + separator + next }.asDocument
}

/**
 * Returns a new document that is [this] with the nesting level (indentation after newlines)
 * increased by [indentationChange]. Negative values for [indentationChange] are allowed, and
 * decrease the nesting level accordingly.
 *
 * ```
 * >>> Document("hello") + (Document.lineBreak + "world").nested()
 * hello
 *     world
 * ```
 *
 * Note that only newlines inside [this] document are affected. For example,
 *
 * ```
 * >>> Document("hello") + Document.lineBreak + (Document("world")).nested()
 * hello
 * world
 * ```
 */
fun PrettyPrintable.nested(indentationChange: Int = 4): Document =
    if (indentationChange == 0)
        this.asDocument
    else
        Nested(this.asDocument, indentationChange)

/**
 * Tries laying out [this] document into a single line by removing the contained
 * [Document.lineBreak]s (and replacing them with spaces); if this does not fit the page, the
 * document is laid out without any changes. This function is the key to layouts that adapt to
 * available space nicely.
 *
 * ```
 * >>> (Document("hello") / "world").grouped()
 * hello world
 * ```
 */
fun PrettyPrintable.grouped(): Document =
    this.asDocument.grouped()

/** See [PrettyPrintable.grouped]. */
private fun Document.grouped(): Document {
    // TODO: simply replacing all soft line breaks with spaces for now.
    //  Do proper layout in the future.
    return when (this) {
        is Empty ->
            this

        is Text ->
            this

        is Concatenated ->
            document1.grouped() + document2.grouped()

        is LineBreak ->
            this

        is SoftLineBreak ->
            Text(" ")

        is Nested ->
            copy(document = document.grouped())

        is Styled ->
            copy(document = document.grouped())
    }
}

/**
 * Returns a new document that is [this] with [style] applied.
 *
 * Styles can be nested.
 */
fun PrettyPrintable.styled(style: Style): Document =
    Styled(this.asDocument, style)

/**
 * Concatenates all the elements separated by [separator] and enclosed in [prefix] and [postfix].
 *
 * The elements are laid out horizontally if that fits the page
 * (note the extra space after [separator]s):
 * ```
 * val docs = listOf("1", "2", "3", "4").map { Document(it) }
 * >>> docs.joined(separator = Document(","), prefix = Document("("), postfix = Document(")"))
 * (1, 2, 3, 4)
 * ```
 *
 * If there is not enough space, the input is split into lines entry-wise with
 * separators at the end:
 * ```
 * >>> docs.joined(separator = Document(","), prefix = Document("("), postfix = Document(")"))
 * (
 * 1,
 * 2,
 * 3,
 * 4)
 * ```
 * Use [nested] to add indentation when the elements are split across lines.
 */
fun List<PrettyPrintable>.joined(
    separator: PrettyPrintable = Document(","),
    prefix: PrettyPrintable = Document(),
    postfix: PrettyPrintable = Document()
): Document {
    return prefix
        .plus(this.concatenated(separator + Document.lineBreak))
        .plus(postfix)
        .grouped()
}

/** Like [joined] but using commas as separators and enclosed in parentheses. */
fun List<PrettyPrintable>.tupled(): Document =
    this.joined(prefix = Document("("), postfix = Document(")"))

/** Like [joined] but using commas as separators and enclosed in square brackets. */
fun List<PrettyPrintable>.bracketed(): Document =
    this.joined(prefix = Document("["), postfix = Document("]"))

/** Like [joined] but using commas as separators and enclosed in curly braces. */
fun List<PrettyPrintable>.braced(): Document =
    this.joined(prefix = Document("{"), postfix = Document("}"))

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
         * The empty document is the left and right unit to [plus]. That is
         * ```
         * Document() + Document("hello") == Document("hello") == Document("hello") + Document()
         * ```
         */
        operator fun invoke(): Document {
            return Empty
        }

        /**
         * Returns the document containing [text] converting all line breaks to [lineBreak].
         * Note that [lineBreak] may be undone by [grouped]:
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
private data class Nested(val document: Document, val indentationChange: Int) : Document() {
    init {
        require(indentationChange != 0)
    }
}

/** Same as [document] but with [style] applied. */
private data class Styled(val document: Document, val style: Style) : Document()

/** A regular expression that recognizes Unicode line breaks. */
private val unicodeLineBreak: Regex = Regex("\\R")
