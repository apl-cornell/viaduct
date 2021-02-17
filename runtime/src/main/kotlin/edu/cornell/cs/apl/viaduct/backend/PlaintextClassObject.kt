package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.UpdateNameNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

sealed class PlaintextClassObject {
    abstract fun query(query: QueryNameNode, arguments: List<Value>): Value

    abstract fun update(update: UpdateNameNode, arguments: List<Value>)
}

object NullObject : PlaintextClassObject() {
    override fun query(query: QueryNameNode, arguments: List<Value>): Value {
        throw Exception("runtime error")
    }

    override fun update(update: UpdateNameNode, arguments: List<Value>) {
        throw Exception("runtime error")
    }
}

class ImmutableCellObject(val value: Value) : PlaintextClassObject() {
    override fun query(query: QueryNameNode, arguments: List<Value>): Value {
        return when (query.value) {
            is Get -> value

            else -> {
                throw Exception("runtime error")
            }
        }
    }

    override fun update(update: UpdateNameNode, arguments: List<Value>) {
        throw Exception("runtime error")
    }
}

class MutableCellObject(var value: Value) : PlaintextClassObject() {
    override fun query(query: QueryNameNode, arguments: List<Value>): Value {
        return when (query.value) {
            is Get -> value

            else -> {
                throw Exception("runtime error")
            }
        }
    }

    override fun update(update: UpdateNameNode, arguments: List<Value>) {
        value = when (val updateValue = update.value) {
            is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                arguments[0]
            }

            is Modify -> {
                updateValue.operator.apply(value, arguments[0])
            }

            else -> {
                throw Exception("runtime error")
            }
        }
    }
}

class VectorObject(val size: Int, defaultValue: Value) : PlaintextClassObject() {
    val values: ArrayList<Value> = ArrayList(size)

    init {
        for (i: Int in 0 until size) {
            values.add(defaultValue)
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
                throw Exception("runtime error")
            }
        }
    }

    override fun update(update: UpdateNameNode, arguments: List<Value>) {
        val index = arguments[0] as IntegerValue

        values[index.value] = when (val updateValue = update.value) {
            is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                arguments[1]
            }

            is Modify -> {
                updateValue.operator.apply(values[index.value], arguments[1])
            }

            else -> {
                throw Exception("runtime error")
            }
        }
    }
}
