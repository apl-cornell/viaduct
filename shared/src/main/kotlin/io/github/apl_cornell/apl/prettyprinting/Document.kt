package io.github.apl_cornell.apl.prettyprinting

import io.github.apl_cornell.apl.prettyprinting.Document.Companion.invoke
import org.fusesource.jansi.Ansi
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/** The maximum number of characters to put on one line. */
const val DEFAULT_LINE_WIDTH = 80

/** An object that has a pretty text representation. */
interface PrettyPrintable {
    /** Returns the pretty text representation of this object. */
    fun toDocument(): Document
}

/**
 * Concatenates [this] and [other].
 *
 * ```
 * >>> Document("hello") + "World"
 * helloWorld
 * ```
 */
operator fun PrettyPrintable.plus(other: PrettyPrintable): Document =
    Concatenated(listOf(this.toDocument(), other.toDocument()))

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
    Concatenated(listOf(this.toDocument(), Document(" "), other.toDocument()))

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
    Concatenated(listOf(this.toDocument(), Document.lineBreak, other.toDocument()))

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
    val documents = this.map { it.toDocument() }.joinedWith(separator.toDocument())
    return Concatenated(documents)
}

/** Returns [this] list with [separator] inserted between each element. */
private fun <T> List<T>.joinedWith(separator: T): List<T> {
    val result = mutableListOf<T>()
    if (this.isNotEmpty())
        result.add(this.first())
    this.drop(1).forEach {
        result.add(separator)
        result.add(it)
    }
    return result
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
    Nested(this.toDocument(), indentationChange)

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
    this.toDocument().grouped()

/** See [PrettyPrintable.grouped]. */
private fun Document.grouped(): Document {
    // TODO: simply replacing all soft line breaks with spaces for now.
    //  Do proper layout in the future.
    return when (this) {
        is Text ->
            this

        is Concatenated ->
            Concatenated(documents.map { it.grouped() })

        is LineBreak ->
            this

        is SoftLineBreak ->
            Text(" ")

        is Nested ->
            Nested(document.grouped(), indentationChange)

        is Styled ->
            Styled(document.grouped(), style)
    }
}

/**
 * Returns a new document that is [this] with [style] applied.
 *
 * Styles can be nested.
 */
fun PrettyPrintable.styled(style: Style): Document =
    Styled(this.toDocument(), style)

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

/** Like [joined] but using commas as separators and enclosed in `/*` and `*/`. */
fun PrettyPrintable.commented(): Document =
    (Document("/*") * this * Document("*/")).grouped()

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
    override fun toDocument(): Document = this

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

                is Concatenated ->
                    documents.forEach { it.traverse(indentation, style) }

                is Nested ->
                    this.document.traverse(indentation + this.indentationChange, style)

                is Styled ->
                    this.document.traverse(indentation, this.style)
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
        PrintStream(byteStream, false, Charsets.UTF_8).use {
            this.print(it, lineWidth, ansi = false)
            return String(byteStream.toByteArray(), Charsets.UTF_8)
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
        operator fun invoke(): Document = Concatenated()

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
            val documents = lines.map { Text(it) }
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

/** A document representing a nonempty single-line string. */
private class Text private constructor(val text: String) : Document() {
    init {
        require(!text.contains(unicodeLineBreak))
    }

    companion object {
        operator fun invoke(text: String): Document =
            if (text.isEmpty()) Concatenated() else Text(text)
    }
}

/** The concatenation of two documents. */
private class Concatenated private constructor(val documents: List<Document>) :
    Document(),
    Iterable<Document> by documents {
    companion object {
        /** The empty document. */
        private val empty = Concatenated(listOf())

        /** Returns the empty document. */
        operator fun invoke(): Document = empty

        operator fun invoke(documents: Iterable<Document>): Document {
            val nonEmpty = documents.filter { empty != it }
            return when (nonEmpty.size) {
                0 -> Concatenated()
                1 -> nonEmpty.first()
                else -> Concatenated(nonEmpty)
            }
        }
    }
}

/** A forced line break. */
private object LineBreak : Document()

/** A line break that can be removed by [PrettyPrintable.grouped]. */
// TODO  We will probably remove this in the future and add something more general.
private object SoftLineBreak : Document()

/** Same as [document] but with its indentation level adjusted by [indentationChange]. */
private class Nested private constructor(val document: Document, val indentationChange: Int) : Document() {
    init {
        require(indentationChange != 0)
    }

    companion object {
        operator fun invoke(document: Document, indentationChange: Int): Document =
            if (indentationChange == 0)
                document
            else
                Nested(document, indentationChange)
    }
}

/** Same as [document] but with [style] applied. */
private class Styled(val document: Document, val style: Style) : Document()

/** A regular expression that recognizes Unicode line breaks. */
private val unicodeLineBreak: Regex = Regex("\\R")
