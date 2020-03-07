package edu.cornell.cs.apl.prettyprinting

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
operator fun PrettyPrintable.plus(other: PrettyPrintable): Document =
    Document(this.asDocument, other.asDocument)

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
    this.asDocument.nested(indentationChange)

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

/**
 * Returns a new document that is [this] with [style] applied.
 *
 * Styles can be nested.
 */
fun PrettyPrintable.styled(style: Style): Document =
    this.asDocument.styled(style)

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
