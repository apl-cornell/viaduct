package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Set
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
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
