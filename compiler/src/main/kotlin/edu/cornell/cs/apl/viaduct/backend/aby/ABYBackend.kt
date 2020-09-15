package edu.cornell.cs.apl.viaduct.backend.aby

import de.tu_darmstadt.cs.encrypto.aby.ABYParty
import de.tu_darmstadt.cs.encrypto.aby.Role
import de.tu_darmstadt.cs.encrypto.aby.Share
import de.tu_darmstadt.cs.encrypto.aby.SharingType
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.ABYCircuitBuilder
import edu.cornell.cs.apl.viaduct.backend.ABYCircuitGate
import edu.cornell.cs.apl.viaduct.backend.ABYConstGate
import edu.cornell.cs.apl.viaduct.backend.ABYDummyInGate
import edu.cornell.cs.apl.viaduct.backend.ABYInGate
import edu.cornell.cs.apl.viaduct.backend.ABYOperation
import edu.cornell.cs.apl.viaduct.backend.ABYOperationGate
import edu.cornell.cs.apl.viaduct.backend.AbstractBackendInterpreter
import edu.cornell.cs.apl.viaduct.backend.HostAddress
import edu.cornell.cs.apl.viaduct.backend.LoopBreakSignal
import edu.cornell.cs.apl.viaduct.backend.ObjectLocation
import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ProtocolProjection
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.UpdateNameNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
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
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThan
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Negation
import edu.cornell.cs.apl.viaduct.syntax.operators.Subtraction
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.SortedSet
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/** Backend for the ABY MPC framework. */
class ABYBackend : ProtocolBackend {
    companion object {
        private const val DEFAULT_PORT = 7766
    }

    private var aby: ABYParty? = null
    private var role: Role? = null

    override fun initialize(connectionMap: Map<Host, HostAddress>, projection: ProtocolProjection) {
        val protocolHosts: Set<Host> = projection.protocol.hosts
        assert(protocolHosts.size == 2)

        val sortedHosts: SortedSet<Host> = protocolHosts.toSortedSet()

        // lowest host is the server
        val otherHost: Host
        if (sortedHosts.first() == projection.host) {
            role = Role.SERVER
            otherHost = sortedHosts.last()
        } else {
            role = Role.CLIENT
            otherHost = sortedHosts.first()
        }

        val otherHostAddress: HostAddress = connectionMap[otherHost]!!
        aby = ABYParty(role, otherHostAddress.ipAddress, DEFAULT_PORT)
    }

    override suspend fun run(
        runtime: ViaductProcessRuntime,
        program: ProgramNode,
        process: BlockNode
    ) {
        if (aby != null || role != null) {
            val interpreter = ABYInterpreter(aby!!, role!!, program, runtime)

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
    private val aby: ABYParty,
    private val role: Role,
    program: ProgramNode,
    private val runtime: ViaductProcessRuntime
) : AbstractBackendInterpreter<ABYInterpreter.ABYClassObject>(program) {
    companion object {
        val BITLEN: Long = 64
    }

    private val typeAnalysis = TypeAnalysis.get(program)

    private val ssTempStoreStack: Stack<PersistentMap<Temporary, ABYCircuitGate>> = Stack()

    private var ssTempStore: PersistentMap<Temporary, ABYCircuitGate>
        get() {
            return ssTempStoreStack.peek()
        }
        set(value) {
            ssTempStoreStack.pop()
            ssTempStoreStack.push(value)
        }

    private val ctTempStoreStack: Stack<PersistentMap<Temporary, Value>> = Stack()

    private var ctTempStore: PersistentMap<Temporary, Value>
        get() {
            return ctTempStoreStack.peek()
        }
        set(value) {
            ctTempStoreStack.pop()
            ctTempStoreStack.push(value)
        }

    init {
        assert(runtime.projection.protocol is ABY)

        objectStoreStack.push(persistentMapOf())
        ssTempStoreStack.push(persistentMapOf())
        ctTempStoreStack.push(persistentMapOf())
    }

    private fun pushContext(
        newObjectStore: PersistentMap<ObjectVariable, ObjectLocation>,
        newSsTempStore: PersistentMap<Temporary, ABYCircuitGate>,
        newCtTempStore: PersistentMap<Temporary, Value>
    ) {
        objectStoreStack.push(newObjectStore)
        ssTempStoreStack.push(newSsTempStore)
        ctTempStoreStack.push(newCtTempStore)
    }

    override fun pushContext() {
        pushContext(objectStore, ssTempStore, ctTempStore)
    }

    override fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf(), persistentMapOf())
    }

    override fun popContext() {
        objectStoreStack.pop()
        ssTempStoreStack.pop()
        ctTempStoreStack.pop()
    }

    override fun getContextMarker(): Int {
        return objectStoreStack.size
    }

    override fun restoreContext(marker: Int) {
        while (objectStoreStack.size > marker) {
            objectStoreStack.pop()
            ssTempStoreStack.pop()
            ctTempStoreStack.pop()
        }
    }

    override fun allocateObject(obj: ABYClassObject): ObjectLocation {
        objectHeap.add(obj)
        return objectHeap.size - 1
    }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): ABYClassObject {
        return ABYImmutableCellObject(runSecretSharedExpr(expr))
    }

    override suspend fun buildObject(
        className: ClassNameNode,
        typeArguments: Arguments<ValueTypeNode>,
        arguments: Arguments<AtomicExpressionNode>
    ): ABYClassObject {
        return when (className.value) {
            ImmutableCell -> {
                val valGate = runSecretSharedExpr(arguments[0])
                ABYImmutableCellObject(valGate)
            }

            MutableCell -> {
                val valGate = runSecretSharedExpr(arguments[0])
                ABYMutableCellObject(valGate)
            }

            Vector -> {
                val length = runExprAsValue(arguments[0]) as IntegerValue
                ABYVectorObject(length.value, typeArguments[0].value.defaultValue)
            }

            else -> throw UndefinedNameError(className)
        }
    }

    override fun getNullObject(): ABYClassObject {
        return ABYNullObject
    }

    private fun valueToCircuit(value: Value, isInput: Boolean = false): ABYCircuitGate {
        return when (value) {
            is BooleanValue ->
                if (isInput) {
                    ABYInGate(if (value.value) 1 else 0)
                } else {
                    ABYConstGate(if (value.value) 1 else 0)
                }

            is IntegerValue ->
                if (isInput) {
                    ABYInGate(value.value)
                } else {
                    ABYConstGate(value.value)
                }

            else -> throw Exception("unknown value type")
        }
    }

    override suspend fun runExprAsValue(expr: AtomicExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value

            is ReadNode ->
                ctTempStore[expr.temporary.value]
                    ?: throw UndefinedNameError(expr.temporary)
        }
    }

    suspend fun runSecretSharedExpr(expr: PureExpressionNode): ABYCircuitGate {
        return when (expr) {
            is LiteralNode -> valueToCircuit(expr.value)

            is ReadNode ->
                ssTempStore[expr.temporary.value]!!

            is OperatorApplicationNode -> {
                val circuitArguments: List<ABYCircuitGate> = expr.arguments.map { arg -> runSecretSharedExpr(arg) }
                operatorToCircuit(expr.operator, circuitArguments)
            }

            is QueryNode -> {
                val loc = getObjectLocation(expr.variable.value)
                getObject(loc).query(expr.query, expr.arguments)
            }

            is DeclassificationNode -> runSecretSharedExpr(expr.expression)
            is EndorsementNode -> runSecretSharedExpr(expr.expression)
        }
    }

    override suspend fun runLet(stmt: LetNode) {
        when (val rhs: ExpressionNode = stmt.value) {
            is ReceiveNode -> {
                val rhsProtocol: Protocol = rhs.protocol.value

                if (rhsProtocol.hosts.contains(runtime.projection.host)) { // actually receive input
                    val receivedValue: Value =
                        runtime.receive(ProtocolProjection(rhsProtocol, runtime.projection.host))

                    ctTempStore = ctTempStore.put(stmt.temporary.value, receivedValue)
                    ssTempStore = ssTempStore.put(stmt.temporary.value, valueToCircuit(receivedValue, isInput = true))
                } else {
                    ssTempStore = ssTempStore.put(stmt.temporary.value, ABYDummyInGate())
                }
            }

            is InputNode -> throw Exception("cannot perform I/O in non-Local protocol")

            is PureExpressionNode ->
                ssTempStore = ssTempStore.put(stmt.temporary.value, runSecretSharedExpr(rhs))
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        getObject(getObjectLocation(stmt.variable.value)).update(stmt.update, stmt.arguments)
    }

    fun buildABYCircuit(outGate: ABYCircuitGate): Share {
        val circuitBuilder =
            ABYCircuitBuilder(
                aby.getCircuitBuilder(SharingType.S_YAO)!!,
                BITLEN,
                role
            )

        // pre-order traversal of circuit
        val traverseStack = Stack<ABYCircuitGate>()

        // post-order traversal of circuit
        val exprStack = Stack<ABYCircuitGate>()

        // actual ABY circuit
        val shareStack = Stack<Share>()

        traverseStack.push(outGate)

        // build post-order traversal
        while (traverseStack.isNotEmpty()) {
            val curGate: ABYCircuitGate = traverseStack.pop()!!
            exprStack.push(curGate)
            for (child: ABYCircuitGate in curGate.children) {
                traverseStack.push(child)
            }
        }

        // "evaluate" stack as a reverse Polish expression by building the ABY circuit
        while (exprStack.isNotEmpty()) {
            val curGate: ABYCircuitGate = exprStack.pop()!!
            val numChildren = curGate.children.size
            val childrenShares: MutableList<Share> = mutableListOf()
            for (i in 1..numChildren) {
                childrenShares.add(shareStack.pop())
            }
            shareStack.push(curGate.buildABYGate(circuitBuilder, childrenShares))
        }

        assert(shareStack.size == 1)
        return circuitBuilder.circuit.putOUTGate(shareStack.peek()!!, Role.ALL)
    }

    // actually perform MPC protocol and declassify output
    override suspend fun runSend(stmt: SendNode) {
        val sendValue: Value =
            when (val msg: AtomicExpressionNode = stmt.message) {
                is LiteralNode -> {
                    msg.value
                }

                is ReadNode -> {
                    val outGate: ABYCircuitGate =
                        ssTempStore[msg.temporary.value]
                            ?: throw UndefinedNameError(msg.temporary)

                    aby.reset()
                    val outShare = buildABYCircuit(outGate)
                    aby.execCircuit()
                    val result = outShare.clearValue32.toInt()

                    when (val msgType: ValueType = typeAnalysis.type(msg)) {
                        is BooleanType -> BooleanValue(result != 0)

                        is IntegerType -> IntegerValue(result)

                        else -> throw Exception("unknown type $msgType")
                    }
                }
            }

        if (stmt.protocol.value.hosts.contains(runtime.projection.host)) {
            runtime.send(sendValue, ProtocolProjection(stmt.protocol.value, runtime.projection.host))
        }
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw Exception("cannot perform I/O in non-Local protocol")
    }

    fun operatorToCircuit(operator: Operator, arguments: List<ABYCircuitGate>): ABYCircuitGate {
        return when (operator) {
            is Negation ->
                ABYOperationGate(
                    ABYOperation.SUB_GATE,
                    listOf(ABYConstGate(0), arguments[0])
                )

            is Addition ->
                ABYOperationGate(
                    ABYOperation.ADD_GATE,
                    listOf(arguments[0], arguments[1])
                )

            is Subtraction ->
                ABYOperationGate(
                    ABYOperation.SUB_GATE,
                    listOf(arguments[0], arguments[1])
                )

            is Multiplication ->
                ABYOperationGate(
                    ABYOperation.MUL_GATE,
                    listOf(arguments[0], arguments[1])
                )

            is Minimum ->
                ABYOperationGate(
                    ABYOperation.MUX_GATE,
                    listOf(
                        ABYOperationGate(ABYOperation.GT_GATE, listOf(arguments[0], arguments[1])),
                        arguments[1],
                        arguments[0]
                    )
                )

            is Maximum ->
                ABYOperationGate(
                    ABYOperation.MUX_GATE,
                    listOf(
                        ABYOperationGate(ABYOperation.GT_GATE, listOf(arguments[0], arguments[1])),
                        arguments[0],
                        arguments[1]
                    )
                )

            // TODO: check if INV gate is actually NOT
            /*
            is Not ->
                ABYOperationGate(
                    ABYOperation.INV_GATE,
                    listOf(arguments[0])
                )
             */

            is And ->
                ABYOperationGate(
                    ABYOperation.AND_GATE,
                    listOf(arguments[0], arguments[1])
                )

            /*
            is Or ->
                ABYOperationGate(
                    ABYOperation.OR_GATE,
                    listOf(arguments[0], arguments[1])
                )
             */

            // (a == b) <--> (a <= b && b <= a)
            is EqualTo ->
                ABYOperationGate(
                    ABYOperation.EQ_GATE,
                    listOf(
                        arguments[0],
                        arguments[1]
                    )
                )

            is LessThan ->
                ABYOperationGate(
                    ABYOperation.GT_GATE,
                    listOf(arguments[1], arguments[0])
                )

            /*
            is LessThanOrEqualTo ->
                ABYOperationGate(
                    ABYOperation.INV_GATE,
                    listOf(
                        ABYOperationGate(
                            ABYOperation.GT_GATE,
                            listOf(arguments[1], arguments[0])
                        )
                    )
                )
             */

            is Mux ->
                ABYOperationGate(
                    ABYOperation.MUX_GATE,
                    listOf(arguments[0], arguments[1], arguments[2])
                )

            else -> throw Exception("operator $operator not supported by ABY backend")
        }
    }

    private abstract class ABYClassObject {
        abstract suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): ABYCircuitGate

        abstract suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>)
    }

    class ABYImmutableCellObject(var gate: ABYCircuitGate) : ABYClassObject() {
        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): ABYCircuitGate {
            return when (query.value) {
                is Get -> gate

                else -> throw Exception("runtime error")
            }
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            throw Exception("runtime error")
        }
    }

    inner class ABYMutableCellObject(var gate: ABYCircuitGate) : ABYClassObject() {
        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): ABYCircuitGate {
            return when (query.value) {
                is Get -> gate

                else -> throw Exception("runtime exception")
            }
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            gate = when (update.value) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    this@ABYInterpreter.runSecretSharedExpr(arguments[0])
                }

                is Modify -> {
                    val circuitArg: ABYCircuitGate = runSecretSharedExpr(arguments[0])
                    operatorToCircuit(update.value.operator, listOf(gate, circuitArg))
                }

                else -> throw Exception("runtime error")
            }
        }
    }

    inner class ABYVectorObject(val size: Int, defaultValue: Value) : ABYClassObject() {
        val gates: ArrayList<ABYCircuitGate> = ArrayList(size)

        init {
            for (i: Int in 0 until size) {
                gates[i] = valueToCircuit(defaultValue)
            }
        }

        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): ABYCircuitGate {
            return when (query.value) {
                is Get -> {
                    val index = runExprAsValue(arguments[0]) as IntegerValue
                    gates[index.value]
                }

                else -> throw Exception("runtime error")
            }
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            val index = runExprAsValue(arguments[0]) as IntegerValue

            gates[index.value] = when (update.value) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    runSecretSharedExpr(arguments[1])
                }

                is Modify -> {
                    val circuitArg: ABYCircuitGate = runSecretSharedExpr(arguments[1])
                    operatorToCircuit(update.value.operator, listOf(gates[index.value], circuitArg))
                }

                else -> throw Exception("runtime error")
            }
        }
    }

    object ABYNullObject : ABYClassObject() {
        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): ABYCircuitGate {
            throw Exception("runtime error")
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            throw Exception("runtime error")
        }
    }
}
