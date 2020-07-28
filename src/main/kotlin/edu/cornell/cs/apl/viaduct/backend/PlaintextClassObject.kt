package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.errors.UnknownMethodError
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.UpdateNameNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

sealed class PlaintextClassObject(
    protected val objectName: ObjectVariableNode,
    protected val objectType: ObjectType
) {
    abstract fun query(query: QueryNameNode, arguments: List<Value>): Value

    abstract fun update(update: UpdateNameNode, arguments: List<Value>)
}

class ImmutableCellObject(
    val value: Value,
    objectName: ObjectVariableNode,
    objectType: ObjectType
) : PlaintextClassObject(objectName, objectType) {
    override fun query(query: QueryNameNode, arguments: List<Value>): Value {
        return when (query.value) {
            is Get -> value

            else -> {
                throw UnknownMethodError(objectName, query, objectType, arguments.map { arg -> arg.type })
            }
        }
    }

    override fun update(update: UpdateNameNode, arguments: List<Value>) {
        throw UnknownMethodError(objectName, update, objectType, arguments.map { arg -> arg.type })
    }
}

class MutableCellObject(
    var value: Value,
    objectName: ObjectVariableNode,
    objectType: ObjectType
) : PlaintextClassObject(objectName, objectType) {
    override fun query(query: QueryNameNode, arguments: List<Value>): Value {
        return when (query.value) {
            is Get -> value

            else -> {
                throw UnknownMethodError(objectName, query, objectType, arguments.map { arg -> arg.type })
            }
        }
    }

    override fun update(update: UpdateNameNode, arguments: List<Value>) {
        value = when (update.value) {
            is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                arguments[0]
            }

            is Modify -> {
                update.value.operator.apply(value, arguments[0])
            }

            else -> {
                throw UnknownMethodError(objectName, update, objectType, arguments.map { arg -> arg.type })
            }
        }
    }
}

class VectorObject(
    val size: Int,
    defaultValue: Value,
    objectName: ObjectVariableNode,
    objectType: ObjectType
) : PlaintextClassObject(objectName, objectType) {
    val values: ArrayList<Value> = ArrayList(size)

    init {
        for (i: Int in 0 until size) {
            values[i] = defaultValue
        }
    }

    override fun query(query: QueryNameNode, arguments: List<Value>): Value {
        return when (query.value) {
            // TODO: fail silently when index is out of bounds
            is Get -> {
                val index = arguments[0] as IntegerValue
                values[index.value]
            }

            else -> {
                throw UnknownMethodError(objectName, query, objectType, arguments.map { arg -> arg.type })
            }
        }
    }

    override fun update(update: UpdateNameNode, arguments: List<Value>) {
        val index = arguments[0] as IntegerValue

        values[index.value] = when (update.value) {
            is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                arguments[1]
            }

            is Modify -> {
                update.value.operator.apply(values[index.value], arguments[1])
            }

            else -> {
                throw UnknownMethodError(objectName, update, objectType, arguments.map { arg -> arg.type })
            }
        }
    }
}
