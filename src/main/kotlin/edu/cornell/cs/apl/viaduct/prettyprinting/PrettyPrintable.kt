package edu.cornell.cs.apl.viaduct.prettyprinting

/** An object that can be pretty printed as a [Document]. */
interface PrettyPrintable {
    /** Pretty prints this object as a [Document]. */
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
    Document(this.toDocument(), other.toDocument())

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
 * Concatenates all elements together with [separator]s in between.
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
    else reduce { acc, next -> acc + separator + next }.toDocument()
}

// TODO: documentation.
fun PrettyPrintable.nested(indentationChange: Int = 4): Document =
    this.toDocument().nested(indentationChange)

// TODO: documentation.
// TODO: test
fun PrettyPrintable.grouped(): Document =
    this.toDocument().grouped()

/**
 * Returns a new document that is [this] with [style] applied.
 *
 * Styles can be nested.
 */
fun PrettyPrintable.styled(style: Style): Document =
    this.toDocument().styled(style)
