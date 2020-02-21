package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import kotlinx.collections.immutable.persistentListOf

/** The type of a vector. */
data class VectorType(val elementType: ValueType, val elementLabel: Label?, val sizeLabel: Label?) :
    ObjectType {
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
