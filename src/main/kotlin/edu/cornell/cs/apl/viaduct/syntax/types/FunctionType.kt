package edu.cornell.cs.apl.viaduct.syntax.types

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** The type of a function or a function like object. */
class FunctionType(arguments: List<ValueType>, val result: ValueType) : Type {
    // Make an immutable copy
    val arguments: List<ValueType> = arguments.toPersistentList()

    constructor(vararg arguments: ValueType, result: ValueType) :
        this(persistentListOf(*arguments), result)

    override fun equals(other: Any?): Boolean {
        if (other !is FunctionType)
            return false
        return this.arguments == other.arguments && this.result == other.result
    }

    override fun hashCode(): Int {
        return Pair(arguments, result).hashCode()
    }
}
