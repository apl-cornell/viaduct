package edu.cornell.cs.apl.viaduct.backend.aby

import de.tu_darmstadt.cs.encrypto.aby.ABYParty
import de.tu_darmstadt.cs.encrypto.aby.Aby
import de.tu_darmstadt.cs.encrypto.aby.Role
import de.tu_darmstadt.cs.encrypto.aby.Share
import de.tu_darmstadt.cs.encrypto.aby.SharingType
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.AbstractProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.HostAddress
import edu.cornell.cs.apl.viaduct.backend.ObjectLocation
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreterFactory
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.UpdateNameNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlin.system.measureTimeMillis
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import mu.KotlinLogging

private var logger = KotlinLogging.logger("ABY")

class ABYProtocolInterpreter(
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val runtime: ViaductProcessRuntime,
    connectionMap: Map<Host, HostAddress>
) : AbstractProtocolInterpreter<ABYProtocolInterpreter.ABYClassObject>(program) {

    private var aby: ABYParty
    private var role: Role
    private var otherHost: Host

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
        val protocol = runtime.projection.protocol as ABY

        when (runtime.projection.host) {
            protocol.server -> {
                role = Role.SERVER
                otherHost = protocol.client
            }
            protocol.client -> {
                role = Role.CLIENT
                otherHost = protocol.server
            }
            else ->
                throw ViaductInterpreterError(
                    "ABY interpreter for protocol ${runtime.projection.protocol.asDocument.print()} " +
                        "cannot execute code for host ${runtime.projection.host.name}"
                )
        }

        val otherHostAddress: HostAddress =
            connectionMap[otherHost]
                ?: throw ViaductInterpreterError("cannot find address for host ${otherHost.name}")

        aby = ABYParty(role, otherHostAddress.ipAddress, DEFAULT_PORT, Aby.getLT(), BITLEN)

        logger.info { "connected ABY to other host at ${otherHostAddress.ipAddress}:$DEFAULT_PORT" }

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

    override suspend fun pushContext() {
        pushContext(objectStore, ssTempStore, ctTempStore)
    }

    override suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf(), persistentMapOf())
    }

    override suspend fun popContext() {
        objectStoreStack.pop()
        ssTempStoreStack.pop()
        ctTempStoreStack.pop()
    }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): ABYClassObject {
        return ABYImmutableCellObject(runSecretSharedExpr(expr))
    }

    override suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): ABYClassObject {
        return when (className) {
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
                ABYVectorObject(length.value, typeArguments[0].defaultValue)
            }

            else -> throw ViaductInterpreterError("ABY: Cannot build object of unknown class $className")
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
                    ABYConstantGate(if (value.value) 1 else 0)
                }

            is IntegerValue ->
                if (isInput) {
                    ABYInGate(value.value)
                } else {
                    ABYConstantGate(value.value)
                }

            else -> throw ViaductInterpreterError("unknown value type: ${value.asDocument.print()}")
        }
    }

    private suspend fun runCleartextRead(read: ReadNode): Value {
        val storeValue = ctTempStore[read.temporary.value]
        return if (storeValue == null) {
            val sendProtocol = protocolAnalysis.primaryProtocol(read)

            if (runtime.projection.protocol != sendProtocol) {
                val events: ProtocolCommunication = protocolAnalysis.relevantCommunicationEvents(read)

                var cleartextInput: Value? = null
                for (event in events) {
                    when {
                        // cleartext input
                        event.recv.id == ABY.CLEARTEXT_INPUT && event.recv.host == runtime.projection.host -> {
                            val receivedValue: Value =
                                runtime.receive(event)

                            if (cleartextInput == null) {
                                cleartextInput = receivedValue
                            } else if (cleartextInput != receivedValue) {
                                throw ViaductInterpreterError("received different values")
                            }
                        }

                        // secret input; throw an error
                        event.recv.id == ABY.SECRET_INPUT ->
                            throw ViaductInterpreterError("ABY: expected cleartext read, got secret read instead", read)
                    }
                }
                assert(cleartextInput != null)
                ctTempStore = ctTempStore.put(read.temporary.value, cleartextInput!!)
                cleartextInput
            } else { // temporary should be stored locally, but isn't
                throw UndefinedNameError(read.temporary)
            }
        } else {
            storeValue
        }
    }

    override suspend fun runExprAsValue(expr: AtomicExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value

            is ReadNode -> runCleartextRead(expr)
        }
    }

    private suspend fun runSecretRead(read: ReadNode): ABYCircuitGate {
        val storeValue = ssTempStore[read.temporary.value]
        return if (storeValue == null) {
            val sendProtocol = protocolAnalysis.primaryProtocol(read)

            if (sendProtocol != runtime.projection.protocol) {
                val events: ProtocolCommunication = protocolAnalysis.relevantCommunicationEvents(read)

                var secretInput: ABYCircuitGate? = null
                var previousReceivedValue: Value? = null
                for (event in events) {
                    when {
                        // secret input for this host; create input gate
                        event.recv.id == ABY.SECRET_INPUT && event.recv.host == runtime.projection.host -> {
                            val receivedValue: Value = runtime.receive(event)

                            if (previousReceivedValue == null) {
                                secretInput = valueToCircuit(receivedValue, isInput = true)
                                previousReceivedValue = receivedValue
                            } else if (previousReceivedValue != receivedValue) {
                                throw ViaductInterpreterError("received different values")
                            }
                        }

                        // other host has secret input; create dummy input gate
                        event.recv.id == ABY.SECRET_INPUT && event.recv.host != runtime.projection.host -> {
                            secretInput = ABYDummyInGate()
                        }

                        // cleartext input; throw an error
                        event.recv.id == ABY.CLEARTEXT_INPUT ->
                            throw ViaductInterpreterError("ABY: expected secret read, got cleartext read instead", read)
                    }
                }
                assert(secretInput != null)
                ssTempStore = ssTempStore.put(read.temporary.value, secretInput!!)
                secretInput
            } else { // temporary should be stored locally, but isn't
                throw UndefinedNameError(read.temporary)
            }
        } else {
            storeValue
        }
    }

    suspend fun runSecretSharedExpr(expr: PureExpressionNode): ABYCircuitGate {
        return when (expr) {
            is LiteralNode -> valueToCircuit(expr.value)

            is ReadNode -> runSecretRead(expr)

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

    private fun buildABYCircuit(outGate: ABYCircuitGate, outRole: Role): Share {
        val circuitBuilder =
            ABYCircuitBuilder(
                arithCircuit = aby.getCircuitBuilder(SharingType.S_ARITH)!!,
                boolCircuit = aby.getCircuitBuilder(SharingType.S_BOOL)!!,
                yaoCircuit = aby.getCircuitBuilder(SharingType.S_YAO)!!,
                bitlen = BITLEN,
                role = role
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
            shareStack.push(curGate.putGate(circuitBuilder, childrenShares))
        }

        assert(shareStack.size == 1)

        return circuitBuilder.circuit(outGate.circuitType).putOUTGate(shareStack.peek()!!, outRole)
    }

    private fun executeABYCircuit(letNode: LetNode, receivingHosts: Set<Host>): Value? {
        val thisHostReceives = receivingHosts.contains(runtime.projection.host)
        val otherHostReceives = receivingHosts.contains(otherHost)
        val outRole: Role =
            when {
                thisHostReceives && !otherHostReceives ->
                    this.role

                !thisHostReceives && otherHostReceives ->
                    if (this.role == Role.SERVER) Role.CLIENT else Role.SERVER

                thisHostReceives && otherHostReceives ->
                    Role.ALL

                else ->
                    throw ViaductInterpreterError("ABY: at least one party must receive output when executing circuit")
            }

        val outputGate: ABYCircuitGate =
            ssTempStore[letNode.temporary.value]
                ?: throw UndefinedNameError(letNode.temporary)

        aby.reset()
        val outShare: Share = buildABYCircuit(outputGate, outRole)

        val execDuration = measureTimeMillis { aby.execCircuit() }

        val result: Int = outShare.clearValue32.toInt()

        logger.info { "executed ABY circuit in ${execDuration}ms, sent output to $outRole" }

        return if (thisHostReceives) {
            when (val msgType: ValueType = typeAnalysis.type(letNode)) {
                is BooleanType -> BooleanValue(result != 0)

                is IntegerType -> IntegerValue(result)

                else -> throw Exception("unknown type $msgType")
            }
        } else null
    }

    override suspend fun runLet(stmt: LetNode) {
        when (val rhs = stmt.value) {
            is ReceiveNode -> throw IllegalInternalCommunicationError(rhs)

            is InputNode -> throw ViaductInterpreterError("cannot perform I/O in non-Local protocol")

            is PureExpressionNode -> {
                val rhsCircuit = runSecretSharedExpr(rhs)
                ssTempStore = ssTempStore.put(stmt.temporary.value, rhsCircuit)

                // execute circuit and broadcast to other protocols
                val readers: Set<SimpleStatementNode> = protocolAnalysis.directRemoteReaders(stmt)

                if (readers.isNotEmpty()) {
                    val events: Set<CommunicationEvent> =
                        readers.fold(setOf()) { acc, reader ->
                            acc.union(
                                protocolAnalysis
                                    .relevantCommunicationEvents(stmt, reader)
                            )
                        }

                    val receivingHosts = events.map { event -> event.recv.host }.toSet()
                    val outValue = executeABYCircuit(stmt, receivingHosts)

                    if (outValue != null) {
                        val hostEvents =
                            events.filter { event -> event.send.asProjection() == runtime.projection }

                        for (event in hostEvents) {
                            runtime.send(outValue, event)
                        }
                    }
                }
            }
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        getObject(getObjectLocation(stmt.variable.value)).update(stmt.update, stmt.arguments)
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw ViaductInterpreterError("cannot perform I/O in non-Local protocol")
    }

    abstract class ABYClassObject {
        abstract suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): ABYCircuitGate

        abstract suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>)
    }

    class ABYImmutableCellObject(private var gate: ABYCircuitGate) : ABYClassObject() {
        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): ABYCircuitGate {
            return when (query.value) {
                is Get -> gate

                else -> {
                    throw ViaductInterpreterError("ABY: unknown query for immutable cell", query)
                }
            }
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            throw ViaductInterpreterError("ABY: unknown update for immutable cell", update)
        }
    }

    inner class ABYMutableCellObject(private var gate: ABYCircuitGate) : ABYClassObject() {
        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): ABYCircuitGate {
            return when (query.value) {
                is Get -> gate

                else -> {
                    throw ViaductInterpreterError("ABY: unknown query for mutable cell", query)
                }
            }
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            gate = when (update.value) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    this@ABYProtocolInterpreter.runSecretSharedExpr(arguments[0])
                }

                is Modify -> {
                    val circuitArg: ABYCircuitGate = runSecretSharedExpr(arguments[0])
                    operatorToCircuit(update.value.operator, listOf(gate, circuitArg))
                }

                else ->
                    throw ViaductInterpreterError("ABY: unknown update for mutable cell", update)
            }
        }
    }

    inner class ABYVectorObject(val size: Int, defaultValue: Value) : ABYClassObject() {
        private val gates: ArrayList<ABYCircuitGate> = ArrayList(size)

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

                else -> throw ViaductInterpreterError("ABY: unknown query ${query.value} for vector", query)
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

                else -> throw ViaductInterpreterError("ABY: unknown update ${update.value} for vector", update)
            }
        }
    }

    object ABYNullObject : ABYClassObject() {
        override suspend fun query(query: QueryNameNode, arguments: List<AtomicExpressionNode>): ABYCircuitGate {
            throw ViaductInterpreterError("ABY: unknown query ${query.value} for null object", query)
        }

        override suspend fun update(update: UpdateNameNode, arguments: List<AtomicExpressionNode>) {
            throw ViaductInterpreterError("ABY: unknown update ${update.value} for null object", update)
        }
    }

    companion object : ProtocolInterpreterFactory {
        private const val DEFAULT_PORT = 7766
        private const val BITLEN: Long = 32

        override fun buildProtocolInterpreter(
            program: ProgramNode,
            protocolAnalysis: ProtocolAnalysis,
            runtime: ViaductProcessRuntime,
            connectionMap: Map<Host, HostAddress>
        ): ProtocolInterpreter {
            return ABYProtocolInterpreter(program, protocolAnalysis, runtime, connectionMap)
        }
    }
}
