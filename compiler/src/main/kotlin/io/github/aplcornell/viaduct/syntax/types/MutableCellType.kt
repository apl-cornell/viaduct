package io.github.aplcornell.viaduct.syntax.types

import io.github.aplcornell.viaduct.syntax.datatypes.ClassName
import io.github.aplcornell.viaduct.syntax.datatypes.Get
import io.github.aplcornell.viaduct.syntax.datatypes.Modify
import io.github.aplcornell.viaduct.syntax.datatypes.MutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.QueryName
import io.github.aplcornell.viaduct.syntax.datatypes.Set
import io.github.aplcornell.viaduct.syntax.datatypes.UpdateName
import kotlinx.collections.immutable.persistentListOf

/** The type of [MutableCell] objects. */
data class MutableCellType(val elementType: ValueType) : ObjectType() {
    override val className: ClassName
        get() = MutableCell

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

    override fun getType(update: UpdateName): FunctionType? {
        return when (update) {
            is Set ->
                FunctionType(elementType, result = UnitType)
            is Modify -> {
                val operatorType = update.operator.type
                if (operatorType.arguments[0] == elementType && operatorType.result == elementType) {
                    FunctionType(operatorType.arguments[1], result = UnitType)
                } else {
                    null
                }
            }
            else ->
                null
        }
    }
}
