package io.github.aplcornell.viaduct.syntax.types

import io.github.aplcornell.viaduct.syntax.datatypes.ClassName
import io.github.aplcornell.viaduct.syntax.datatypes.Get
import io.github.aplcornell.viaduct.syntax.datatypes.ImmutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.QueryName
import io.github.aplcornell.viaduct.syntax.datatypes.UpdateName
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
