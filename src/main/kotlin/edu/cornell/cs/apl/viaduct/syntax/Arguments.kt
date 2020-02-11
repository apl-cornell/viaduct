package edu.cornell.cs.apl.viaduct.syntax

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A list of arguments. */
class Arguments<out T>(arguments: List<T>, override val sourceLocation: SourceLocation) :
    HasSourceLocation, List<T> by arguments {
    // Make an immutable copy
    val arguments: List<T> = arguments.toPersistentList()

    /** An empty argument list. */
    constructor(sourceLocation: SourceLocation) : this(persistentListOf(), sourceLocation)

    companion object {
        /**
         * Constructs an [Arguments] instance while inferring source location from [arguments].
         * Source location is the location of the first argument merged with the location of the
         * last argument.
         *
         * [arguments] must be non-empty.
         */
        @JvmStatic
        fun <T : HasSourceLocation> from(vararg arguments: T): Arguments<T> {
            require(arguments.isNotEmpty()) { "Cannot infer source location without arguments." }
            val sourceLocation =
                arguments.first().sourceLocation.merge(arguments.last().sourceLocation)
            return Arguments(persistentListOf(*arguments), sourceLocation)
        }
    }
}
