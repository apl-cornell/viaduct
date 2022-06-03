package io.github.apl_cornell.viaduct.syntax

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A list of arguments. */
class Arguments<out T>
private constructor(
    private val arguments: PersistentList<T>,
    override val sourceLocation: SourceLocation
) : HasSourceLocation, List<T> by arguments {
    /** An empty argument list. */
    constructor(sourceLocation: SourceLocation) :
        this(persistentListOf(), sourceLocation)

    constructor(arguments: List<T>, sourceLocation: SourceLocation) :
        this(arguments.toPersistentList(), sourceLocation)

    companion object {
        /**
         * Constructs an [Arguments] instance while inferring source location from [arguments].
         * Source location is the location of the first argument merged with the location of the
         * last argument.
         *
         * [arguments] must be non-empty.
         */
        // TODO: what about parentheses around arguments?
        @JvmStatic
        fun <T : HasSourceLocation> from(vararg arguments: T): Arguments<T> {
            require(arguments.isNotEmpty()) { "Cannot infer source location without arguments." }
            val sourceLocation =
                arguments.first().sourceLocation.merge(arguments.last().sourceLocation)
            return Arguments(persistentListOf(*arguments), sourceLocation)
        }
    }
}
