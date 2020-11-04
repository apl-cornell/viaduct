package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
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
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
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

    override suspend fun pushFunctionContext(
        arguments: PersistentMap<ParameterNode, Pair<Protocol, FunctionArgumentNode>>
    ) {
        functionFrameStack.push(
            objectHeap.size to
                persistentMapOf(
                    *(arguments.map { kv -> kv.key to kv.value.second }.toTypedArray())
                )
        )

        val initialStore: PersistentMap<ObjectVariable, ObjectLocation> =
            arguments
                .map { kv ->
                    val objectLoc =
                        when (val argument = kv.value.second) {
                            is ExpressionArgumentNode ->
                                allocateObject(
                                    buildExpressionObject(
                                        kv.value.first,
                                        argument.expression
                                    )
                                )

                            is ObjectReferenceArgumentNode ->
                                getObjectLocation(argument.variable.value)

                            is ObjectDeclarationArgumentNode ->
                                allocateObject(getNullObject(kv.value.first))

                            is OutParameterArgumentNode ->
                                allocateObject(getNullObject(kv.value.first))
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
        objectHeap.subList(heapTail, objectHeap.size).clear()
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

    override fun getContextMarker(): Int {
        return objectStoreStack.size
    }

    override suspend fun restoreContext(marker: Int) {
        while (getContextMarker() > marker) {
            popContext()
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

    protected fun allocateObject(obj: Obj): ObjectLocation {
        objectHeap.add(obj)
        return objectHeap.size - 1
    }

    abstract suspend fun buildExpressionObject(protocol: Protocol, expr: AtomicExpressionNode): Obj

    abstract suspend fun buildObject(
        protocol: Protocol,
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): Obj

    abstract fun getNullObject(protocol: Protocol): Obj

    override suspend fun runSimpleStatement(protocol: Protocol, stmt: SimpleStatementNode) {
        when (stmt) {
            is LetNode -> runLet(protocol, stmt)

            is DeclarationNode -> {
                putObjectLocation(
                    stmt.name.value,
                    allocateObject(
                        buildObject(
                            protocol,
                            stmt.className.value,
                            stmt.typeArguments.map { it.value },
                            stmt.arguments
                        )
                    )
                )
            }

            is UpdateNode -> runUpdate(protocol, stmt)

            is OutParameterInitializationNode -> {
                val obj: Obj =
                    when (val initializer = stmt.initializer) {
                        is OutParameterExpressionInitializerNode -> {
                            buildExpressionObject(protocol, initializer.expression)
                        }

                        is OutParameterConstructorInitializerNode -> {
                            buildObject(
                                protocol,
                                initializer.className.value,
                                initializer.typeArguments.map { it.value },
                                initializer.arguments
                            )
                        }
                    }

                putObject(getObjectLocation(stmt.name.value), obj)
            }

            is OutputNode -> runOutput(protocol, stmt)

            is SendNode -> throw IllegalInternalCommunicationError(stmt)
        }
    }

    abstract suspend fun runLet(protocol: Protocol, stmt: LetNode)

    abstract suspend fun runUpdate(protocol: Protocol, stmt: UpdateNode)

    abstract suspend fun runOutput(protocol: Protocol, stmt: OutputNode)
}

/** Interpreter for a single protocol.
 *  This class is defined for convenience, so that the
 *  protocol argument on implemented methods isn't necessary. */
abstract class SingleProtocolInterpreter<Obj>(
    program: ProgramNode,
    private val protocol: Protocol
) : AbstractProtocolInterpreter<Obj>(program) {
    override val availableProtocols: Set<Protocol> =
        setOf(protocol)

    abstract suspend fun buildExpressionObject(expr: AtomicExpressionNode): Obj

    override suspend fun buildExpressionObject(protocol: Protocol, expr: AtomicExpressionNode): Obj =
        buildExpressionObject(expr)

    abstract suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): Obj

    override suspend fun buildObject(
        protocol: Protocol,
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): Obj =
        buildObject(className, typeArguments, arguments)

    abstract fun getNullObject(): Obj

    override fun getNullObject(protocol: Protocol): Obj =
        getNullObject()

    abstract suspend fun runGuard(expr: AtomicExpressionNode): Value

    override suspend fun runGuard(protocol: Protocol, expr: AtomicExpressionNode): Value =
        runGuard(expr)

    abstract suspend fun runLet(stmt: LetNode)

    override suspend fun runLet(protocol: Protocol, stmt: LetNode) =
        runLet(stmt)

    abstract suspend fun runUpdate(stmt: UpdateNode)

    override suspend fun runUpdate(protocol: Protocol, stmt: UpdateNode) =
        runUpdate(stmt)

    abstract suspend fun runOutput(stmt: OutputNode)

    override suspend fun runOutput(protocol: Protocol, stmt: OutputNode) =
        runOutput(stmt)
}
