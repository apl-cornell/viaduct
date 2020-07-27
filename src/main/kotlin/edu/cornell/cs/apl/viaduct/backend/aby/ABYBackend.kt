package edu.cornell.cs.apl.viaduct.backend.aby

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.AbstractBackendInterpreter
import edu.cornell.cs.apl.viaduct.backend.HostAddress
import edu.cornell.cs.apl.viaduct.backend.LoopBreakSignal
import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ProtocolProjection
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.backend.ViaductRuntime
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.UnknownMethodError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.UpdateNameNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.PureExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.SortedSet
import java.util.Stack

class ABYBackend : ProtocolBackend {
    companion object {
        const val DEFAULT_PORT = 7766
    }

    init {
        System.loadLibrary("ViaductABY")
    }

    private var aby: ViaductABYParty? = null

    override val supportedProtocols = setOf(ABY.protocolName)

    override fun initialize(connectionMap: Map<Host, HostAddress>, projection: ProtocolProjection) {
        val protocolHosts: Set<Host> = projection.protocol.hosts
        assert(protocolHosts.size == 2)

        val sortedHosts: SortedSet<Host> = protocolHosts.toSortedSet()

        // lowest host is the server
        val role: ABYRole
        val otherHost: Host
        if (sortedHosts.first() == projection.host) {
            role = ABYRole.ABY_SERVER
            otherHost = sortedHosts.last()
        } else {
            role = ABYRole.ABY_CLIENT
            otherHost = sortedHosts.first()
        }

        val otherHostAddress: HostAddress = connectionMap[otherHost]!!
        aby = ViaductABYParty(role, otherHostAddress.ipAddress, DEFAULT_PORT)
    }

    override suspend fun run(
        nameAnalysis: NameAnalysis,
        typeAnalysis: TypeAnalysis,
        runtime: ViaductRuntime,
        program: ProgramNode,
        process: BlockNode,
        projection: ProtocolProjection
    ) {
        if (aby != null) {
            val interpreter =
                ABYInterpreter(
                    aby!!,
                    nameAnalysis, typeAnalysis,
                    ViaductProcessRuntime(runtime, projection),
                    program,
                    projection
                )

            try {
                interpreter.run(process)
            } catch (signal: LoopBreakSignal) {
                throw ViaductInterpreterError(
                    "uncaught loop break signal with jump label ${signal.jumpLabel}", signal.breakNode
                )
            } finally {
                aby!!.delete()
            }
        } else {
            throw Exception("Could not initialize ABY backend")
        }
    }
}

private class ABYInterpreter(
    val aby: ViaductABYParty,
    val nameAnalysis: NameAnalysis,
    val typeAnalysis: TypeAnalysis,
    val runtime: ViaductProcessRuntime,
    val program: ProgramNode,
    val projection: ProtocolProjection
) : AbstractBackendInterpreter() {

    private val objectStoreStack: Stack<MutableMap<ObjectVariable, ABYClassObject>> = Stack()

    private val ssObjectStore: MutableMap<ObjectVariable, ABYClassObject>
        get() {
            assert(!objectStoreStack.empty())
            return objectStoreStack.peek()
        }

    private val ssTempStoreStack: Stack<MutableMap<Temporary, CircuitGate>> = Stack()

    private val ssTempStore: MutableMap<Temporary, CircuitGate>
        get() {
            assert(!ssTempStoreStack.empty())
            return ssTempStoreStack.peek()
        }

    private val ctTempStoreStack: Stack<MutableMap<Temporary, Value>> = Stack()

    private val ctTempStore: MutableMap<Temporary, Value>
        get() {
            assert(!ctTempStoreStack.empty())
            return ctTempStoreStack.peek()
        }

    init {
        objectStoreStack.push(mutableMapOf())
        ssTempStoreStack.push(mutableMapOf())
        ctTempStoreStack.push(mutableMapOf())

        assert(projection.protocol is ABY)
    }

    override fun pushContext() {
        objectStoreStack.push(ssObjectStore.toMutableMap())
        ssTempStoreStack.push(ssTempStore.toMutableMap())
        ctTempStoreStack.push(ctTempStore.toMutableMap())
    }

    override fun popContext() {
        objectStoreStack.pop()
        ssTempStoreStack.pop()
        ctTempStoreStack.pop()
    }

    private fun valueToCircuit(value: Value, isInput: Boolean = false): CircuitGate {
        return when (value) {
            is BooleanValue ->
                if (isInput) {
                    aby.PutINGate(if (value.value) 1 else 0)
                } else {
                    aby.PutCONSTGate(if (value.value) 1 else 0)
                }

            is IntegerValue ->
                if (isInput) {
                    aby.PutINGate(value.value)
                } else {
                    aby.PutCONSTGate(value.value)
                }

            else -> throw Exception("unknown value type")
        }
    }

    override suspend fun runAtomicExpr(expr: AtomicExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value

            is ReadNode ->
                ctTempStore[expr.temporary.value]
                    ?: throw UndefinedNameError(expr.temporary)
        }
    }

    suspend fun runSecretSharedExpr(expr: PureExpressionNode): CircuitGate {
        return when (expr) {
            is LiteralNode -> valueToCircuit(expr.value)

            is ReadNode ->
                ssTempStore[expr.temporary.value]
                    ?: throw UndefinedNameError(expr.temporary)

            is OperatorApplicationNode -> {
                val circuitArguments: List<CircuitGate> = expr.arguments.map { arg -> runSecretSharedExpr(arg) }
                operatorToCircuit(expr.operator, circuitArguments)
            }

            is QueryNode ->
                ssObjectStore[expr.variable.value]
                    ?.query(expr.query, expr.arguments)
                    ?: throw UndefinedNameError(expr.variable)

            is DeclassificationNode -> runSecretSharedExpr(expr.expression)
            is EndorsementNode -> runSecretSharedExpr(expr.expression)
        }
    }

    override suspend fun runDeclaration(stmt: DeclarationNode) {
        val argumentValues: List<Value> = stmt.arguments.map { arg -> runAtomicExpr(arg) }

        return when (val objectType: ObjectType = typeAnalysis.type(stmt)) {
            is ImmutableCellType -> {
                ssObjectStore[stmt.variable.value] =
                    ABYImmutableCellObject(argumentValues[0], stmt.variable, objectType)
            }

            is MutableCellType -> {
                ssObjectStore[stmt.variable.value] =
                    ABYMutableCellObject(argumentValues[0], stmt.variable, objectType)
            }

            is VectorType -> {
                val length = argumentValues[0] as IntegerValue
                ssObjectStore[stmt.variable.value] =
                    ABYVectorObject(length.value, objectType.elementType.defaultValue, stmt.variable, objectType)
            }

            else -> throw UndefinedNameError(stmt.className)
        }
    }

    override suspend fun runLet(stmt: LetNode) {
        when (val rhs: ExpressionNode = stmt.value) {
            is ReceiveNode -> {
                val rhsProtocol: Protocol = rhs.protocol.value

                if (rhsProtocol.hosts.contains(projection.host)) { // actually receive input
                    val receivedValue: Value =
                        runtime.receive(ProtocolProjection(rhsProtocol, projection.host))

                    ctTempStore[stmt.temporary.value] = receivedValue
                    ssTempStore[stmt.temporary.value] = valueToCircuit(receivedValue, isInput = true)
                } else {
                    ssTempStore[stmt.temporary.value] = aby.PutDummyINGate()
                }
            }

            is InputNode -> throw Exception("cannot perform I/O in non-Local protocol")

            is PureExpressionNode ->
                ssTempStore[stmt.temporary.value] = runSecretSharedExpr(rhs)
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        ssObjectStore[stmt.variable.value]
            ?.update(stmt.update, stmt.arguments)
            ?: throw UndefinedNameError(stmt.variable)
    }

    // actually perform MPC protocol and declassify output
    override suspend fun runSend(stmt: SendNode) {
        if (stmt.protocol.value.hosts.contains(projection.host)) {
            when (val msg: AtomicExpressionNode = stmt.message) {
                is LiteralNode -> {
                    runtime.send(msg.value, ProtocolProjection(stmt.protocol.value, projection.host))
                }

                is ReadNode -> {
                    val outGate: CircuitGate =
                        ssTempStore[msg.temporary.value]
                            ?: throw UndefinedNameError(msg.temporary)

                    aby.Reset()
                    val result: Int = aby.ExecCircuit(outGate)

                    val resultValue: Value =
                        when (val msgType: ValueType = typeAnalysis.type(msg)) {
                            is BooleanType -> BooleanValue(result != 0)

                            is IntegerType -> IntegerValue(result)

                            else -> throw Exception("unknown type $msgType")
                        }

                    runtime.send(resultValue, ProtocolProjection(stmt.protocol.value, projection.host))
                }
            }
        }
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw Exception("cannot perform I/O in non-Local protocol")
    }

    fun operatorToCircuit(operator: Operator, arguments: List<CircuitGate>): CircuitGate {
        return when (operator) {
            // is Negation ->

            is Addition -> aby.PutADDGate(arguments[0], arguments[1])

            // is Subtraction ->

            is Multiplication -> aby.PutMULGate(arguments[0], arguments[1])

            // is Minimum ->

            // is Maximum ->

            // is Not ->

            // is And ->

            // is Or ->

            // is EqualTo ->

            // is LessThan ->

            // is LessThanOrEqualTo ->

            // is Mux ->

            else -> throw Exception("operator $operator not supported by ABY backend")
        }
    }

    private abstract class ABYClassObject(
        protected val objectName: ObjectVariableNode,
        protected val objectType: ObjectType
    ) {
        abstract suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): CircuitGate

        abstract suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>)
    }

    inner class ABYImmutableCellObject(
        value: Value,
        objectName: ObjectVariableNode,
        objectType: ObjectType
    ) : ABYClassObject(objectName, objectType) {

        val gate: CircuitGate = valueToCircuit(value)

        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): CircuitGate {
            return when (query.value) {
                is Get -> gate

                else -> {
                    throw UnknownMethodError(objectName, query, objectType,
                        arguments.map { arg -> this@ABYInterpreter.typeAnalysis.type(arg) })
                }
            }
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            throw UnknownMethodError(objectName, update, objectType,
                arguments.map { arg -> this@ABYInterpreter.typeAnalysis.type(arg) })
        }
    }

    inner class ABYMutableCellObject(
        value: Value,
        objectName: ObjectVariableNode,
        objectType: ObjectType
    ) : ABYClassObject(objectName, objectType) {
        var gate: CircuitGate = valueToCircuit(value)

        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): CircuitGate {
            return when (query.value) {
                is Get -> gate

                else -> {
                    throw UnknownMethodError(objectName, query, objectType,
                        arguments.map { arg -> this@ABYInterpreter.typeAnalysis.type(arg) })
                }
            }
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            gate = when (update.value) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    this@ABYInterpreter.runSecretSharedExpr(arguments[0])
                }

                is Modify -> {
                    val circuitArg: CircuitGate = runSecretSharedExpr(arguments[0])
                    operatorToCircuit(update.value.operator, listOf(gate, circuitArg))
                }

                else -> {
                    throw UnknownMethodError(objectName, update, objectType,
                        arguments.map { arg -> this@ABYInterpreter.typeAnalysis.type(arg) })
                }
            }
        }
    }

    inner class ABYVectorObject(
        val size: Int,
        defaultValue: Value,
        objectName: ObjectVariableNode,
        objectType: ObjectType
    ) : ABYClassObject(objectName, objectType) {
        val gates: MutableList<CircuitGate> = mutableListOf()

        init {
            for (i: Int in 0 until size) {
                gates.add(valueToCircuit(defaultValue))
            }
        }

        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): CircuitGate {
            return when (query.value) {
                is Get -> {
                    val index = runAtomicExpr(arguments[0]) as IntegerValue
                    gates[index.value]
                }

                else -> {
                    throw UnknownMethodError(objectName, query, objectType,
                        arguments.map { arg -> this@ABYInterpreter.typeAnalysis.type(arg) })
                }
            }
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            val index = runAtomicExpr(arguments[0]) as IntegerValue

            gates[index.value] = when (update.value) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    runSecretSharedExpr(arguments[1])
                }

                is Modify -> {
                    val circuitArg: CircuitGate = runSecretSharedExpr(arguments[1])
                    operatorToCircuit(update.value.operator, listOf(gates[index.value], circuitArg))
                }

                else -> {
                    throw UnknownMethodError(objectName, update, objectType,
                        arguments.map { arg -> this@ABYInterpreter.typeAnalysis.type(arg) })
                }
            }
        }
    }
}
