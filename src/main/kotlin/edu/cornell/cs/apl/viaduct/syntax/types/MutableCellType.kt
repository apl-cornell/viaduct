package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/** The type of a mutable cell. */
data class MutableCellType(val elementType: ValueType, val elementLabel: Label?) :
    ObjectType {
    private val queries =
        persistentMapOf(
            Pair(Get, FunctionType(result = elementType))
        )

    private val updates =
        persistentMapOf(
            Pair(Set, FunctionType(elementType, result = UnitType))
        )

    override val constructorArguments: List<ValueType>
        get() = persistentListOf(elementType)

    override fun getType(query: QueryName): FunctionType? {
        return queries[query]
    }

    override fun getType(update: UpdateName): FunctionType? {
        // TODO: Pair(Modify, UpdateInfo(persistentListOf(IntegerType, elementType)))
        return updates[update]
    }
}
