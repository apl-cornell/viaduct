package io.github.aplcornell.viaduct.attributes

/**
 * Represents an optional value.
 * An [Option] is either [Some], meaning it has a value, or [None], meaning it has no value.
 */
internal sealed class Option<out T>

/** An [Option] that has the value [value]. */
internal data class Some<T>(val value: T) : Option<T>()

/** An [Option] that has no value. */
internal object None : Option<Nothing>()
