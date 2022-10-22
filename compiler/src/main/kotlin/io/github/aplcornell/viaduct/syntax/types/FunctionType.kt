package io.github.aplcornell.viaduct.syntax.types

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.prettyprinting.tupled
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** The type of a function or a function like object. */
class FunctionType
private constructor(
    val arguments: PersistentList<ValueType>,
    val result: ValueType
) : Type {
    constructor(arguments: List<ValueType>, result: ValueType) :
        this(arguments.toPersistentList(), result)

    constructor(vararg arguments: ValueType, result: ValueType) :
        this(persistentListOf(*arguments), result)

    override fun equals(other: Any?): Boolean {
        return other is FunctionType &&
            this.arguments == other.arguments && this.result == other.result
    }

    override fun hashCode(): Int {
        return Pair(arguments, result).hashCode()
    }

    override fun toDocument(): Document = (arguments.map { arg -> arg.toDocument() }.tupled()) * Document("->") * result
}
