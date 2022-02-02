package edu.cornell.cs.apl.viaduct.lowering

import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

sealed class InterpreterObject {
    abstract fun query(query: QueryName, arguments: List<Value>): Value

    abstract fun update(update: UpdateName, arguments: List<Value>): InterpreterObject
}

object NullObject : InterpreterObject() {
    override fun query(query: QueryName, arguments: List<Value>): Value {
        throw Exception("runtime error")
    }

    override fun update(update: UpdateName, arguments: List<Value>): InterpreterObject {
        throw Exception("runtime error")
    }
}

data class ImmutableCellObject(val value: Value) : InterpreterObject() {
    override fun query(query: QueryName, arguments: List<Value>): Value {
        return when (query) {
            is Get -> value

            else -> {
                throw Exception("unknown query ${query.name} for immutable cell")
            }
        }
    }

    override fun update(update: UpdateName, arguments: List<Value>): InterpreterObject {
        throw Exception("cannot update immutable cell")
    }
}

data class MutableCellObject(var value: Value) : InterpreterObject() {
    override fun query(query: QueryName, arguments: List<Value>): Value {
        return when (query) {
            is Get -> this.value

            else -> {
                throw Exception("unknown query ${query.name} for mutable cell")
            }
        }
    }

    override fun update(update: UpdateName, arguments: List<Value>): InterpreterObject {
        return MutableCellObject(
            when (update) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    arguments[0]
                }

                is Modify -> {
                    update.operator.apply(value, arguments[0])
                }

                else -> {
                    throw Exception("unknown update ${update.name} for mutable cell")
                }
            }
        )
    }
}

data class VectorObject(val size: Int, defaultValue: Value) : InterpreterObject() {
    val values: PersistentList<Value> = persistentListOf()

    init {
        for (i: Int in 0 until size) {
            values = values.add(defaultValue)
        }
    }

    override fun query(query: QueryName, arguments: List<Value>): Value {
        return when (query) {
            // TODO: fail silently when index is out of bounds
            is Get -> {
                val index = arguments[0] as IntegerValue
                values[index.value]
            }

            else -> {
                throw Exception("unknown query ${query.name} for vector")
            }
        }
    }

    override fun update(update: UpdateName, arguments: List<Value>): InterpreterObject {
        val index = arguments[0] as IntegerValue

        values[index.value] = when (update) {
            is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                arguments[1]
            }

            is Modify -> {
                update.operator.apply(values[index.value], arguments[1])
            }

            else -> {
                throw Exception("unknown update ${update.name} for vector")
            }
        }
    }
}

data class PartialStore(
    val objectStore: PersistentMap<ObjectVariable, InterpreterObject>,
    val temporaryStore: PersistentMap<Temporary, LoweredExpression>
) {
    fun updateObject(variable: ObjectVariable, obj: InterpreterObject): PartialStore {
        return this.copy(objectStore = objectStore.put(variable, obj))
    }

    fun deleteObject(variable: ObjectVariable): PartialStore {
        return this.copy(objectStore = objectStore.remove(variable))
    }

    fun updateTemporary(temporary: Temporary, expr: LoweredExpression): PartialStore {
        return this.copy(temporaryStore = temporaryStore.put(temporary, expr))
    }
}

private fun LoweredExpression.isStatic(): Boolean = this is LiteralNode

class PartialEvaluator(
    val program: FlowchartProgram
) {
    companion object {
        fun evaluate(program: FlowchartProgram) {
            PartialEvaluator(program).evaluate()
        }
    }

    private fun evaluate(store: PartialStore, expr: LoweredExpression): LoweredExpression {
        return when (expr) {
            is LiteralNode -> expr

            is ReadNode -> {
                store.temporaryStore[expr.temporary] ?:
                    throw Error("cannot find temporary ${expr.temporary.name}")
            }

            is OperatorApplicationNode -> {
                val reducedArgs = expr.arguments.map { evaluate(store, it) }
                val staticArgs = reducedArgs.all { it.isStatic() }
                if (staticArgs) {
                    val valArgs = reducedArgs.map { (it as LiteralNode).value }
                    LiteralNode(expr.operator.apply(valArgs))

                } else {
                    expr.copy(arguments = reducedArgs.toPersistentList())
                }
            }

            is QueryNode -> {
                val reducedArgs = expr.arguments.map { evaluate(store, it) }
                val staticArgs = reducedArgs.all { it.isStatic() }
                if (staticArgs && store.objectStore.contains(expr.variable)) {
                    val valArgs = reducedArgs.map { (it as LiteralNode).value }
                    val queryVal = store.objectStore[expr.variable]!!.query(expr.query, valArgs)
                    LiteralNode(queryVal)

                } else {
                    expr.copy(arguments = reducedArgs.toPersistentList())
                }
            }

            is InputNode -> expr
        }
    }

    private fun buildObject(
        className: ClassName, typeArguments: List<ValueType>, arguments: List<Value>
    ): InterpreterObject {
        return when (className) {
            ImmutableCell -> ImmutableCellObject(arguments[0])

            MutableCell -> MutableCellObject(arguments[0])

            Vector -> {
                val length = (arguments[0] as IntegerValue).value
                VectorObject(length, typeArguments[0].defaultValue)
            }

            else -> throw Exception("unknown class name $className")
        }
    }

    private fun evaluate(store: PartialStore, stmt: LoweredStatement): Pair<PartialStore, LoweredStatement> {
        return when (stmt) {
            is SkipNode -> Pair(store, stmt)

            is DeclarationNode -> {
                val reducedArgs = stmt.arguments.map { arg -> evaluate(store, arg) }
                val staticArgs = reducedArgs.all { it.isStatic() }
                if (staticArgs) {
                    val valArgs = reducedArgs.map { (it as LiteralNode).value }
                    val obj = buildObject(stmt.className, stmt.typeArguments, valArgs)
                    Pair(store.updateObject(stmt.name, obj), SkipNode)

                } else {
                    val reducedDecl = stmt.copy(arguments = reducedArgs.toPersistentList())
                    Pair(store, reducedDecl)
                }
            }

            is LetNode -> {
                val reducedExpr = evaluate(store, stmt.value)
                Pair(store.updateTemporary(stmt.temporary, reducedExpr), SkipNode)
            }

            is OutputNode -> {
                val reducedMsg = evaluate(store, stmt.message)
                Pair(store, stmt.copy(message = reducedMsg))
            }

            is UpdateNode -> {
                val reducedArgs = stmt.arguments.map { arg -> evaluate(store, arg) }
                val staticArgs = reducedArgs.all { it.isStatic() }
                if (staticArgs && store.objectStore.contains(stmt.variable)) {
                    val valArgs = reducedArgs.map { (it as LiteralNode).value }
                    store.objectStore[stmt.variable]!!.update(stmt.update, valArgs)
                    Pair(store, SkipNode)

                } else {
                    Pair(store.deleteObject())
                }
            }
        }
    }
}
