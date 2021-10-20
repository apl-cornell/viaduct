package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import kotlinx.collections.immutable.persistentListOf

/** The type of [ImmutableCell] objects. */
data class ImmutableCellType(val elementType: ValueType) : ObjectType() {
    override val className: ClassName
        get() = ImmutableCell

    override val typeArguments: List<ValueType>
        get() = persistentListOf(elementType)

    override val constructorArguments: List<ValueType>
        get() = persistentListOf(elementType)

    override fun getType(query: QueryName): FunctionType? {
        return when (query) {
            is Get ->
                FunctionType(result = elementType)
            else ->
                null
        }
    }

    override fun getType(update: UpdateName): FunctionType? = null
}
