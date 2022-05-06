package edu.cornell.cs.apl.viaduct.lowering

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.bracketed
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
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
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.toPersistentList
import java.util.LinkedList
import java.util.Queue

sealed class InterpreterObject : PrettyPrintable {
    abstract fun query(query: QueryName, arguments: List<Value>): Value

    abstract fun update(update: UpdateName, arguments: List<Value>): InterpreterObject
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

    override fun toDocument(): Document = value.toDocument()
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

    override fun toDocument(): Document = value.toDocument()
}

data class VectorObject(val values: PersistentMap<Int, Value>) : InterpreterObject() {
    constructor(size: Int, defaultValue: Value) : this(
        persistentHashMapOf<Int, Value>(
            *generateSequence(0) { it + 1 }
                .take(size)
                .map { i -> i to defaultValue }
                .toList().toTypedArray()
        )
    )

    override fun query(query: QueryName, arguments: List<Value>): Value {
        return when (query) {
            // TODO: fail silently when index is out of bounds
            is Get -> {
                val index = (arguments[0] as IntegerValue).value
                values[index]!!
            }

            else -> {
                throw Exception("unknown query ${query.name} for vector")
            }
        }
    }

    override fun update(update: UpdateName, arguments: List<Value>): InterpreterObject {
        val index = (arguments[0] as IntegerValue).value

        val value =
            when (update) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    arguments[1]
                }

                is Modify -> {
                    update.operator.apply(values[index]!!, arguments[1])
                }

                else -> {
                    throw Exception("unknown update ${update.name} for vector")
                }
            }

        return this.copy(values = this.values.put(index, value))
    }

    override fun toDocument(): Document =
        values.map { kv ->
            Document(kv.key.toString()) + Document(":") * kv.value.toDocument()
        }.bracketed()
}

data class PartialStore(
    val objectStore: PersistentMap<ObjectVariable, InterpreterObject>,
    val temporaryStore: PersistentMap<Temporary, LoweredExpression>
) : PrettyPrintable {
    fun getObject(variable: ObjectVariable): InterpreterObject? = objectStore[variable]

    fun updateObject(variable: ObjectVariable, obj: InterpreterObject): PartialStore {
        return this.copy(objectStore = objectStore.put(variable, obj))
    }

    fun deleteObject(variable: ObjectVariable): PartialStore {
        return this.copy(objectStore = objectStore.remove(variable))
    }

    fun updateTemporary(temporary: Temporary, expr: LoweredExpression): PartialStore {
        return this.copy(temporaryStore = temporaryStore.put(temporary, expr))
    }

    override fun toDocument(): Document =
        objectStore.map { kv -> Document(kv.key.name) * Document("=>") * kv.value }.bracketed() *
            temporaryStore.map { kv -> Document(kv.key.name) * Document("=>") * kv.value }.bracketed()
}

private fun LoweredExpression.isStatic(): Boolean = this is LiteralNode

class PartialEvaluator(
    val program: FlowchartProgram
) {
    companion object {
        fun evaluate(program: FlowchartProgram): FlowchartProgram {
            return PartialEvaluator(program).evaluate()
        }
    }

    private val objectBindingTimeMap: Map<ObjectVariable, BindingTime> = BindingTimeAnalysis.computeBindingTime(program)
    private val objectRenameMap = mutableMapOf<ObjectVariable, ObjectVariable>()
    private val nameGenerator = FreshNameGenerator()

    private fun getObjectName(variable: ObjectVariable) = objectRenameMap[variable] ?: variable

    private fun evaluateExpression(store: PartialStore, expr: LoweredExpression): LoweredExpression {
        return when (expr) {
            is LiteralNode -> expr

            is ReadNode -> {
                store.temporaryStore[expr.temporary]
                    ?: throw Error("cannot find temporary ${expr.temporary.name}")
            }

            is OperatorApplicationNode -> {
                val reducedArgs = expr.arguments.map { evaluateExpression(store, it) }
                val staticArgs = reducedArgs.all { it.isStatic() }
                if (staticArgs) {
                    val valArgs = reducedArgs.map { (it as LiteralNode).value }
                    LiteralNode(expr.operator.apply(valArgs))
                } else {
                    expr.copy(arguments = reducedArgs.toPersistentList())
                }
            }

            is QueryNode -> {
                val reducedArgs = expr.arguments.map { evaluateExpression(store, it) }
                val staticArgs = reducedArgs.all { it.isStatic() }
                if (staticArgs && store.objectStore.contains(expr.variable)) {
                    val valArgs = reducedArgs.map { (it as LiteralNode).value }
                    val queryVal = store.objectStore[expr.variable]!!.query(expr.query, valArgs)
                    LiteralNode(queryVal)
                } else {
                    expr.copy(
                        variable = objectRenameMap[expr.variable] ?: expr.variable,
                        arguments = reducedArgs.toPersistentList()
                    )
                }
            }

            is InputNode -> expr
        }
    }

    private fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<Value>
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

    /* Evaluate a statement in a given partial store,
    returning an updated store and a residual statement. */
    private fun evaluate(store: PartialStore, stmt: LoweredStatement): Pair<PartialStore, LoweredStatement> {
        return when (stmt) {
            is SkipNode -> Pair(store, stmt)

            is DeclarationNode -> {
                val reducedArgs = stmt.arguments.map { arg -> evaluateExpression(store, arg) }
                val staticArgs = reducedArgs.all { it.isStatic() }
                val staticObject = objectBindingTimeMap[stmt.name] == BindingTime.STATIC

                val objName =
                    if (objectRenameMap.containsKey(stmt.name)) {
                        val freshName = nameGenerator.getFreshName(stmt.name.name)
                        objectRenameMap[stmt.name] = ObjectVariable(freshName)
                        ObjectVariable(freshName)
                    } else {
                        objectRenameMap[stmt.name] = stmt.name
                        stmt.name
                    }

                if (staticArgs && staticObject) {
                    val valArgs = reducedArgs.map { (it as LiteralNode).value }
                    val obj = buildObject(stmt.className, stmt.typeArguments, valArgs)
                    Pair(store.updateObject(objName, obj), SkipNode)
                } else {
                    Pair(
                        store,
                        stmt.copy(name = objName, arguments = reducedArgs.toPersistentList())
                    )
                }
            }

            is LetNode -> {
                val reducedExpr = evaluateExpression(store, stmt.value)
                Pair(store.updateTemporary(stmt.temporary, reducedExpr), SkipNode)
            }

            is OutputNode -> {
                val reducedMsg = evaluateExpression(store, stmt.message)
                Pair(store, stmt.copy(message = reducedMsg))
            }

            is UpdateNode -> {
                val reducedArgs = stmt.arguments.map { arg -> evaluateExpression(store, arg) }
                val staticArgs = reducedArgs.all { it.isStatic() }
                val staticObject = objectBindingTimeMap[stmt.variable]!! == BindingTime.STATIC

                when {
                    // easy case: interpret update as in normal (full) evaluation
                    staticArgs && staticObject -> {
                        val valArgs = reducedArgs.map { (it as LiteralNode).value }
                        val updatedObj =
                            store.getObject(getObjectName(stmt.variable))!!
                                .update(stmt.update, valArgs)
                        Pair(store.updateObject(getObjectName(stmt.variable), updatedObj), SkipNode)
                    }

                    !staticArgs && staticObject -> {
                        when (val obj = store.getObject(getObjectName(stmt.variable))!!) {
                            is ImmutableCellObject ->
                                throw Exception("cannot update immutable cell object")

                            // for mutable cells, remove cell from partial store
                            is MutableCellObject -> {
                                Pair(
                                    store.deleteObject(getObjectName(stmt.variable)),
                                    stmt.copy(
                                        variable = getObjectName(stmt.variable),
                                        arguments = reducedArgs.toPersistentList()
                                    )
                                )
                            }

                            is VectorObject -> {
                                val reducedIndex = reducedArgs[0]
                                val reducedRHS = reducedArgs[1]

                                when {
                                    // if index is dynamic, remove all entries in vector
                                    !reducedIndex.isStatic() -> {
                                        Pair(
                                            store.updateObject(
                                                getObjectName(stmt.variable),
                                                VectorObject(persistentHashMapOf())
                                            ),
                                            stmt.copy(arguments = reducedArgs.toPersistentList())
                                        )
                                    }

                                    // if index is static but arg is dynamic, remove single index in vector
                                    reducedIndex.isStatic() && !reducedRHS.isStatic() -> {
                                        val index = ((reducedIndex as LiteralNode).value as IntegerValue).value
                                        Pair(
                                            store.updateObject(
                                                getObjectName(stmt.variable),
                                                obj.copy(values = obj.values.remove(index))
                                            ),
                                            stmt.copy(arguments = reducedArgs.toPersistentList())
                                        )
                                    }

                                    else -> throw Exception("unreachable state")
                                }
                            }
                        }
                    }

                    // add static object to partial store
                    // TODO: don't determine class name by number of arguments
                    staticArgs && !staticObject -> {
                        when (stmt.update) {
                            // set can be evaluated because it does not require a read
                            is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                                if (reducedArgs.size == 1) { // mutable cell
                                    Pair(
                                        store.updateObject(
                                            getObjectName(stmt.variable),
                                            MutableCellObject((reducedArgs[0] as LiteralNode).value)
                                        ),
                                        SkipNode
                                    )
                                } else { // vector
                                    val index = ((reducedArgs[0] as LiteralNode).value as IntegerValue).value
                                    val value = (reducedArgs[1] as LiteralNode).value
                                    Pair(
                                        store.updateObject(
                                            getObjectName(stmt.variable),
                                            VectorObject(persistentHashMapOf(index to value))
                                        ),
                                        SkipNode
                                    )
                                }
                            }

                            // modify requires a read so it can't be evaluated
                            is Modify -> {
                                Pair(store, stmt.copy(arguments = reducedArgs.toPersistentList()))
                            }

                            else -> throw Exception("unknown update ${stmt.update.name}")
                        }
                    }

                    // args and object are dynamic, can't be evaluated
                    else -> {
                        Pair(store, stmt.copy(arguments = reducedArgs.toPersistentList()))
                    }
                }
            }
        }
    }

    /* Evaluate a basic block starting a given partial store,
    returning an updated store, residual block, and labels of successor blocks */
    private fun evaluate(
        store: PartialStore,
        block: LoweredBasicBlock<RegularBlockLabel>
    ): LoweredBasicBlock<ResidualBlockLabel> {
        var curStore = store
        val residualStmts = mutableListOf<LoweredStatement>()

        // partially execute statements in block
        for (stmt in block.statements) {
            val (newStore, residualStmt) = evaluate(curStore, stmt)
            curStore = newStore
            if (residualStmt != SkipNode) {
                residualStmts.add(residualStmt)
            }
        }

        // calculate successors
        val residualJump: LoweredControl<ResidualBlockLabel> =
            when (val jump = block.jump) {
                is Goto -> {
                    Goto(ResidualBlockLabel(jump.label, curStore))
                }

                is GotoIf -> {
                    val reducedGuard = evaluateExpression(curStore, jump.guard)
                    if (reducedGuard.isStatic()) {
                        val guardValue = ((reducedGuard as LiteralNode).value as BooleanValue).value
                        val successor = if (guardValue) jump.thenLabel else jump.elseLabel
                        Goto(ResidualBlockLabel(successor, curStore))
                    } else {
                        GotoIf(
                            reducedGuard,
                            ResidualBlockLabel(jump.thenLabel, curStore),
                            ResidualBlockLabel(jump.elseLabel, curStore)
                        )
                    }
                }

                is RegularHalt -> ResidualHalt

                // Kotlin compiler is too dumb to recognize that ResidualHalt
                // is not a valid case, so we need this else case
                else -> TODO()
            }

        return LoweredBasicBlock(residualStmts, residualJump)
    }

    /** Offline partial evaluation of a flowchart program. */
    fun evaluate(
        initialStore: PartialStore =
            PartialStore(objectStore = persistentHashMapOf(), temporaryStore = persistentHashMapOf())
    ): FlowchartProgram {
        val initialBlockLabel = ResidualBlockLabel(ENTRY_POINT_LABEL, initialStore)
        val residualBlockMap = mutableMapOf<ResidualBlockLabel, LoweredBasicBlock<ResidualBlockLabel>>()
        val worklist: Queue<ResidualBlockLabel> = LinkedList()
        worklist.add(initialBlockLabel)

        while (!worklist.isEmpty()) {
            val residualBlockLabel = worklist.remove()
            val residualBlock = evaluate(residualBlockLabel.store, program.blocks[residualBlockLabel.label]!!)
            residualBlockMap[residualBlockLabel] = residualBlock

            worklist.addAll(
                residualBlock.successors().filter { !residualBlockMap.containsKey(it) }
            )
        }

        // create fresh labels from label-partial store pairs
        val nameGenerator = FreshNameGenerator()

        val finalLabelMap = mutableMapOf<ResidualBlockLabel, RegularBlockLabel>()
        finalLabelMap[initialBlockLabel] = ENTRY_POINT_LABEL
        for (residualLabel in residualBlockMap.keys) {
            val newLabel = RegularBlockLabel(nameGenerator.getFreshName(residualLabel.label.label))
            finalLabelMap[residualLabel] = newLabel
        }

        val finalBlockMap = mutableMapOf<RegularBlockLabel, LoweredBasicBlock<RegularBlockLabel>>()
        for (kv in residualBlockMap) {
            val relabeledJump: LoweredControl<RegularBlockLabel> =
                when (val jump = kv.value.jump) {
                    is Goto -> Goto(finalLabelMap[jump.label]!!)
                    is GotoIf -> GotoIf(jump.guard, finalLabelMap[jump.thenLabel]!!, finalLabelMap[jump.elseLabel]!!)
                    is ResidualHalt -> RegularHalt

                    // Kotlin compiler is too dumb to recognize that RegularHalt
                    // is not a valid case, so we need this else case
                    else -> TODO()
                }

            finalBlockMap[finalLabelMap[kv.key]!!] = LoweredBasicBlock(kv.value.statements, relabeledJump)
        }

        return FlowchartProgram(blocks = finalBlockMap)
    }
}
