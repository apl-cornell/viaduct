package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/** The type of a vector. */
data class VectorType(val elementType: ValueType, val elementLabel: Label?, val sizeLabel: Label?) :
    ObjectType {
    private val queries =
        persistentMapOf(
            Pair(Get, FunctionType(IntegerType, result = elementType))
        )

    private val updates =
        persistentMapOf(
            Pair(Set, FunctionType(IntegerType, elementType, result = UnitType))
        )

    override val constructorArguments: List<ValueType>
        get() = persistentListOf(IntegerType)

    override fun getType(query: QueryName): FunctionType? {
        return queries[query]
    }

    override fun getType(update: UpdateName): FunctionType? {
        // TODO: Pair(Modify, UpdateInfo(persistentListOf(IntegerType, elementType)))
        return updates[update]
    }
}
