package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionOutputArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

typealias ObjectLocation = Int

abstract class AbstractProtocolInterpreter<Obj>(
    val program: ProgramNode
) : ProtocolInterpreter {
    protected val objectHeap: MutableList<Obj> = mutableListOf()

    private val functionFrameStack: Stack<Pair<Int, PersistentMap<ParameterNode, FunctionArgumentNode>>> = Stack()

    protected val objectStoreStack: Stack<PersistentMap<ObjectVariable, ObjectLocation>> = Stack()

    protected var objectStore: PersistentMap<ObjectVariable, ObjectLocation>
        get() {
            return objectStoreStack.peek()
        }
        set(value) {
            objectStoreStack.pop()
            objectStoreStack.push(value)
        }

    protected abstract suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>)

    override suspend fun pushFunctionContext(arguments: PersistentMap<ParameterNode, FunctionArgumentNode>) {
        functionFrameStack.push(Pair(Integer.max(objectHeap.size - 1, 0), arguments))

        val initialStore: PersistentMap<ObjectVariable, ObjectLocation> =
            arguments
                .map { kv ->
                    val objectLoc =
                        when (val argument = kv.value) {
                            is ExpressionArgumentNode ->
                                allocateObject(buildExpressionObject(argument.expression))

                            is ObjectReferenceArgumentNode ->
                                getObjectLocation(argument.variable.value)

                            is ObjectDeclarationArgumentNode ->
                                allocateObject(getNullObject())

                            is OutParameterArgumentNode ->
                                allocateObject(getNullObject())
                        }
                    kv.key.name.value to objectLoc
                }.toMap().toPersistentMap()

        pushContext(initialStore)
    }

    override suspend fun popFunctionContext() {
        val functionFrame: Pair<Int, PersistentMap<ParameterNode, FunctionArgumentNode>> = functionFrameStack.pop()
        val heapTail: Int = functionFrame.first
        val arguments: PersistentMap<ParameterNode, FunctionArgumentNode> = functionFrame.second

        val outArguments =
            arguments
                .filter { kv -> kv.value is FunctionOutputArgumentNode }
                .map { kv ->
                    Pair(
                        kv.value as FunctionOutputArgumentNode,
                        getObject(getObjectLocation(kv.key.name.value))
                    )
                }

        // destroy function frame
        objectHeap.subList(heapTail, objectHeap.size - 1).clear()
        popContext()

        // point output parameters to new objects
        for (outArgumentPair in outArguments) {
            when (val outArgument: FunctionOutputArgumentNode = outArgumentPair.first) {
                is ObjectDeclarationArgumentNode -> {
                    val loc = allocateObject(outArgumentPair.second)
                    putObjectLocation(outArgument.name.value, loc)
                }

                is OutParameterArgumentNode -> {
                    val loc = getObjectLocation(outArgument.parameter.value)
                    putObject(loc, outArgumentPair.second)
                }
            }
        }
    }

    protected fun getObjectLocation(obj: ObjectVariable): ObjectLocation {
        return objectStore[obj]
            ?: throw ViaductInterpreterError("undefined variable: $obj")
    }

    protected fun putObjectLocation(obj: ObjectVariable, loc: ObjectLocation) {
        objectStore = objectStore.put(obj, loc)
    }

    protected fun getObject(loc: ObjectLocation): Obj {
        return objectHeap[loc]
            ?: throw ViaductInterpreterError("no object at location $loc")
    }

    protected fun putObject(loc: ObjectLocation, obj: Obj) {
        objectHeap[loc] = obj
    }

    abstract fun allocateObject(obj: Obj): ObjectLocation

    abstract suspend fun buildExpressionObject(expr: AtomicExpressionNode): Obj

    abstract suspend fun buildObject(
        className: ClassNameNode,
        typeArguments: Arguments<ValueTypeNode>,
        arguments: Arguments<AtomicExpressionNode>
    ): Obj

    abstract fun getNullObject(): Obj

    override suspend fun runSimpleStatement(stmt: SimpleStatementNode) {
        when (stmt) {
            is LetNode -> runLet(stmt)

            is DeclarationNode -> {
                putObjectLocation(
                    stmt.name.value,
                    allocateObject(
                        buildObject(stmt.className, stmt.typeArguments, stmt.arguments)
                    )
                )
            }

            is UpdateNode -> runUpdate(stmt)

            is OutParameterInitializationNode -> {
                val obj: Obj =
                    when (val initializer = stmt.initializer) {
                        is OutParameterExpressionInitializerNode -> {
                            buildExpressionObject(initializer.expression)
                        }

                        is OutParameterConstructorInitializerNode -> {
                            buildObject(initializer.className, initializer.typeArguments, initializer.arguments)
                        }
                    }

                putObject(getObjectLocation(stmt.name.value), obj)
            }

            is OutputNode -> runOutput(stmt)

            is SendNode -> throw IllegalInternalCommunicationError(stmt)
        }
    }

    abstract suspend fun runLet(stmt: LetNode)

    abstract suspend fun runUpdate(stmt: UpdateNode)

    abstract suspend fun runOutput(stmt: OutputNode)
}
