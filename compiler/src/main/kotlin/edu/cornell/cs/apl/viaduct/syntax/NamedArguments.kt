package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.UnexpectedArgumentError
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

/** A list of labelled arguments. */
class NamedArguments<out T>
private constructor(
    private val argumentLabels: PersistentList<ArgumentLabelNode>,
    private val arguments: PersistentMap<ArgumentLabel, T>,
    override val sourceLocation: SourceLocation
) : HasSourceLocation {
    /** An empty argument list. */
    constructor(sourceLocation: SourceLocation) :
        this(persistentListOf(), persistentMapOf(), sourceLocation)

    /**
     * Returns the argument associated with label [label].
     *
     * @throws UndefinedNameError if there is no argument with label [label].
     */
    operator fun get(label: ArgumentLabel): T {
        return arguments[label] ?: throw UndefinedNameError(Located(label, sourceLocation))
    }

    /**
     * Asserts that the argument list only contains arguments with labels in [expectedArguments].
     *
     * @throws UnexpectedArgumentError if the argument list contains extra arguments.
     */
    fun assertHasNoExtraArguments(expectedArguments: Set<ArgumentLabel>) {
        argumentLabels.forEach {
            if (!expectedArguments.contains(it.value)) {
                throw UnexpectedArgumentError(it)
            }
        }
    }

    companion object {
        @JvmStatic
        operator fun <T> invoke(
            arguments: List<Pair<ArgumentLabelNode, T>>,
            sourceLocation: SourceLocation
        ): NamedArguments<T> {
            // Check for duplicates
            arguments.fold(NameMap<ArgumentLabel, Unit>(), { map, argument -> map.put(argument.first, Unit) })

            val argumentLabels = arguments.map { it.first }.toPersistentList()
            val argumentsMap = arguments.fold(
                persistentMapOf<ArgumentLabel, T>(),
                { map, arg -> map.put(arg.first.value, arg.second) }
            )

            return NamedArguments(argumentLabels, argumentsMap, sourceLocation)
        }
    }
}
