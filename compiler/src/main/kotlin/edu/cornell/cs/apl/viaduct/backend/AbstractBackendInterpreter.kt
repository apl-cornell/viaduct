package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionInputArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionOutputArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.putAll

typealias ObjectLocation = Int

/** Abstract backend interpreter that interprets control flow statements only. */
abstract class AbstractBackendInterpreter<Obj>(val program: ProgramNode) {
    protected val objectHeap: MutableList<Obj> = mutableListOf()

    private val functionFrameStack: Stack<Int> = Stack()

    protected val objectStoreStack: Stack<PersistentMap<ObjectVariable, ObjectLocation>> = Stack()

    protected var objectStore: PersistentMap<ObjectVariable, ObjectLocation>
        get() {
            return objectStoreStack.peek()
        }
        set(value) {
            objectStoreStack.pop()
            objectStoreStack.push(value)
        }

    abstract fun pushContext()

    abstract fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>)

    abstract fun popContext()

    abstract fun getContextMarker(): Int

    abstract fun restoreContext(marker: Int)

    private fun pushFunctionContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore)
        functionFrameStack.push(objectHeap.size)
    }

    private fun popFunctionContext() {
        popContext()
        val heapTail = functionFrameStack.pop()
        objectHeap.subList(heapTail - 1, objectHeap.size - 1).clear()
    }

    protected fun getObjectLocation(obj: ObjectVariable): ObjectLocation {
        return objectStore[obj]!!
    }

    protected fun putObjectLocation(obj: ObjectVariable, loc: ObjectLocation) {
        objectStore = objectStore.put(obj, loc)
    }

    protected fun getObject(loc: ObjectLocation): Obj {
        return objectHeap[loc]!!
    }

    protected fun putObject(loc: ObjectLocation, obj: Obj) {
        objectHeap[loc] = obj
    }

    abstract fun allocateObject(obj: Obj): ObjectLocation

    abstract suspend fun buildExpressionObject(expr: AtomicExpressionNode): Obj

    abstract suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): Obj

    abstract fun getNullObject(): Obj

    abstract suspend fun runExprAsValue(expr: AtomicExpressionNode): Value

    abstract suspend fun runLet(stmt: LetNode)

    abstract suspend fun runUpdate(stmt: UpdateNode)

    abstract suspend fun runSend(stmt: SendNode)

    abstract suspend fun runOutput(stmt: OutputNode)

    suspend fun run(stmt: StatementNode) {
        when (stmt) {
            is DeclarationNode -> {
                putObjectLocation(
                    stmt.name.value,
                    allocateObject(
                        buildObject(
                            stmt.className.value,
                            stmt.typeArguments.map { it.value }, stmt.arguments
                        )
                    )
                )
            }

            is LetNode -> runLet(stmt)

            is UpdateNode -> runUpdate(stmt)

            is OutParameterInitializationNode -> {
                val obj: Obj =
                    when (val initializer = stmt.initializer) {
                        is OutParameterExpressionInitializerNode -> {
                            buildExpressionObject(initializer.expression)
                        }

                        is OutParameterConstructorInitializerNode -> {
                            buildObject(
                                initializer.className.value,
                                initializer.typeArguments.map { it.value }, initializer.arguments
                            )
                        }
                    }

                putObject(getObjectLocation(stmt.name.value), obj)
            }

            is FunctionCallNode -> {
                val inputArguments: List<Obj> =
                    stmt.arguments
                        .filterIsInstance<FunctionInputArgumentNode>()
                        .map { arg ->
                            when (arg) {
                                is ExpressionArgumentNode -> {
                                    buildExpressionObject(arg.expression)
                                }

                                is ObjectReferenceArgumentNode -> {
                                    getObject(getObjectLocation(arg.variable.value))
                                }
                            }
                        }

                val function = program.functionMap[stmt.name.value]!!

                val outputParameters =
                    function.parameters
                        .filter { param -> param.isOutParameter }

                // new frame for function only contains parameters
                val initialObjStore =
                    function.parameters
                        .filter { param -> param.isInParameter }
                        .zip(inputArguments)
                        .fold(persistentMapOf<ObjectVariable, ObjectLocation>()) { acc, pair ->
                            val loc: ObjectLocation = allocateObject(pair.second)
                            acc.put(pair.first.name.value, loc)
                        }
                        .putAll(
                            function.parameters
                                .filter { param -> param.isOutParameter }
                                .map { param -> Pair(param.name.value, allocateObject(getNullObject())) }
                        )

                // push new frame with only parameters
                pushFunctionContext(initialObjStore)

                // execute function body with new frame
                run(function.body)

                // retrieve output parameter objects before popping frame
                val outputParameterObjects: List<Obj> =
                    outputParameters.map { param -> getObject(getObjectLocation(param.name.value)) }

                // pop new frame
                popFunctionContext()

                // point output parameters to new objects
                stmt.arguments
                    .filterIsInstance<FunctionOutputArgumentNode>()
                    .zip(outputParameterObjects)
                    .forEach { pair ->
                        when (val arg = pair.first) {
                            is ObjectDeclarationArgumentNode -> {
                                val loc = allocateObject(pair.second)
                                putObjectLocation(arg.name.value, loc)
                            }

                            is OutParameterArgumentNode -> {
                                val loc = getObjectLocation(arg.parameter.value)
                                putObject(loc, pair.second)
                            }
                        }
                    }
            }

            is SendNode -> runSend(stmt)

            is OutputNode -> runOutput(stmt)

            is IfNode -> {
                val guardVal = runExprAsValue(stmt.guard) as BooleanValue

                if (guardVal.value) {
                    run(stmt.thenBranch)
                } else {
                    run(stmt.elseBranch)
                }
            }

            is InfiniteLoopNode -> {
                // communicate loop break by exception
                val contextMarker: Int = getContextMarker()

                try {
                    run(stmt.body)
                    run(stmt)
                } catch (signal: LoopBreakSignal) { // catch loop break signal
                    // this signal is for an outer loop
                    if (signal.jumpLabel != stmt.jumpLabel.value) {
                        throw signal
                    } else { // restore context
                        restoreContext(contextMarker)
                    }
                }
            }

            is BreakNode -> throw LoopBreakSignal(stmt)

            is BlockNode -> {
                pushContext()

                for (childStmt: StatementNode in stmt) {
                    run(childStmt)
                }

                popContext()
            }
        }
    }
}
