package io.github.apl_cornell.viaduct.syntax.types

import io.github.apl_cornell.viaduct.syntax.datatypes.ClassName
import io.github.apl_cornell.viaduct.syntax.datatypes.Get
import io.github.apl_cornell.viaduct.syntax.datatypes.Modify
import io.github.apl_cornell.viaduct.syntax.datatypes.QueryName
import io.github.apl_cornell.viaduct.syntax.datatypes.Set
import io.github.apl_cornell.viaduct.syntax.datatypes.UpdateName
import io.github.apl_cornell.viaduct.syntax.datatypes.Vector
import kotlinx.collections.immutable.persistentListOf

/** The type of a [Vector] object. */
data class VectorType(val elementType: ValueType) : ObjectType() {
    override val className: ClassName
        get() = Vector

    override val typeArguments: List<ValueType>
        get() = persistentListOf(elementType)

    override val constructorArguments: List<ValueType>
        get() = persistentListOf(IntegerType)

    override fun getType(query: QueryName): FunctionType? {
        return when (query) {
            is Get ->
                FunctionType(IntegerType, result = elementType)
            else ->
                null
        }
    }

    override fun getType(update: UpdateName): FunctionType? {
        return when (update) {
            is Set ->
                FunctionType(IntegerType, elementType, result = UnitType)
            is Modify -> {
                val operatorType = update.operator.type
                if (operatorType.arguments[0] == elementType && operatorType.result == elementType) {
                    FunctionType(IntegerType, operatorType.arguments[1], result = UnitType)
                } else {
                    null
                }
            }
            else ->
                null
        }
    }
}
