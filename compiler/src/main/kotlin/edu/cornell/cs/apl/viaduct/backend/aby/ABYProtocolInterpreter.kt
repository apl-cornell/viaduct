package edu.cornell.cs.apl.viaduct.backend.aby

import de.tu_darmstadt.cs.encrypto.aby.ABYParty
import de.tu_darmstadt.cs.encrypto.aby.Aby
import de.tu_darmstadt.cs.encrypto.aby.Phase
import de.tu_darmstadt.cs.encrypto.aby.Role
import de.tu_darmstadt.cs.encrypto.aby.Share
import de.tu_darmstadt.cs.encrypto.aby.SharingType
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.AbstractProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.HostAddress
import edu.cornell.cs.apl.viaduct.backend.ObjectLocation
import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ViaductRuntime
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.ArithABY
import edu.cornell.cs.apl.viaduct.protocols.BoolABY
import edu.cornell.cs.apl.viaduct.protocols.YaoABY
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
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
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
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

sealed class ABYValue
data class ABYCleartextValue(val value: Value) : ABYValue()
data class ABYSecretValue(val value: ABYCircuitGate) : ABYValue()

class ABYProtocolInterpreter(
    private val host: Host,
    private val otherHost: Host,
    private val role: Role,
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val runtime: ViaductRuntime,
    connectionMap: Map<Host, HostAddress>,
    port: Int = DEFAULT_PORT
) : AbstractProtocolInterpreter<ABYProtocolInterpreter.ABYClassObject>(program) {
    override val availableProtocols: Set<Protocol> =
        if (role == Role.SERVER) {
            setOf(ArithABY(host, otherHost), BoolABY(host, otherHost), YaoABY(host, otherHost))
        } else {
            setOf(ArithABY(otherHost, host), BoolABY(otherHost, host), YaoABY(otherHost, host))
        }

    private val typeAnalysis = TypeAnalysis.get(program)
    private val aby: ABYParty

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
        val otherHostAddress: HostAddress =
            connectionMap[otherHost]
                ?: throw ViaductInterpreterError("cannot find address for host ${otherHost.name}")

        aby = ABYParty(role, otherHostAddress.ipAddress, port, Aby.getLT(), BITLEN)

        logger.info { "connected ABY to other host at ${otherHostAddress.ipAddress}:$port" }

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

    override suspend fun buildExpressionObject(
        protocol: Protocol,
        expr: AtomicExpressionNode
    ): ABYClassObject {
        return ABYImmutableCellObject(
            runSecretExpr(protocolCircuitType[protocol.protocolName]!!, expr)
        )
    }

    override suspend fun buildObject(
        protocol: Protocol,
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): ABYClassObject {
        val circuitType = protocolCircuitType[protocol.protocolName]!!
        return when (className) {
            ImmutableCell -> {
                val valGate = runSecretExpr(circuitType, arguments[0])
                ABYImmutableCellObject(valGate)
            }

            MutableCell -> {
                val valGate = runSecretExpr(circuitType, arguments[0])
                ABYMutableCellObject(valGate)
            }

            Vector -> {
                val length = runPlaintextExpr(arguments[0]) as IntegerValue
                ABYVectorObject(
                    protocolCircuitType[protocol.protocolName]!!,
                    length.value,
                    typeArguments[0].defaultValue
                )
            }

            else -> throw ViaductInterpreterError("ABY: Cannot build object of unknown class $className")
        }
    }

    override fun getNullObject(protocol: Protocol): ABYClassObject {
        return ABYNullObject
    }

    private fun valueToCircuit(circuitType: ABYCircuitType, value: Value, isInput: Boolean = false): ABYCircuitGate {
        return when (value) {
            is BooleanValue ->
                if (isInput) {
                    ABYInGate(if (value.value) 1 else 0, circuitType)
                } else {
                    ABYConstantGate(if (value.value) 1 else 0, circuitType)
                }

            is IntegerValue ->
                if (isInput) {
                    ABYInGate(value.value, circuitType)
                } else {
                    ABYConstantGate(value.value, circuitType)
                }

            else -> throw ViaductInterpreterError("unknown value type: ${value.asDocument.print()}")
        }
    }

    private suspend fun runPlaintextRead(read: ReadNode): Value {
        val storeValue = ctTempStore[read.temporary.value]
        return if (storeValue == null) {
            val sendProtocol = protocolAnalysis.primaryProtocol(read)

            if (!availableProtocols.contains(sendProtocol)) {
                val events: ProtocolCommunication = protocolAnalysis.relevantCommunicationEvents(read)

                var cleartextInput: Value? = null
                for (event in events) {
                    when {
                        // cleartext input
                        event.recv.id == ABY.CLEARTEXT_INPUT && event.recv.host == host -> {
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

    private suspend fun runPlaintextExpr(expr: AtomicExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value
            is ReadNode -> runPlaintextRead(expr)
        }
    }

    private suspend fun runSecretRead(circuitType: ABYCircuitType, read: ReadNode): ABYCircuitGate {
        val storeValue = ssTempStore[read.temporary.value]
        return if (storeValue == null) {
            val sendProtocol = protocolAnalysis.primaryProtocol(read)

            if (!availableProtocols.contains(sendProtocol)) {
                val events: ProtocolCommunication = protocolAnalysis.relevantCommunicationEvents(read)

                var secretInput: ABYCircuitGate? = null
                var previousReceivedValue: Value? = null
                for (event in events) {
                    when {
                        // secret input for this host; create input gate
                        event.recv.id == ABY.SECRET_INPUT && event.recv.host == host -> {
                            val receivedValue: Value = runtime.receive(event)

                            if (previousReceivedValue == null) {
                                secretInput = valueToCircuit(circuitType, receivedValue, isInput = true)
                                previousReceivedValue = receivedValue
                            } else if (previousReceivedValue != receivedValue) {
                                throw ViaductInterpreterError("received different values")
                            }
                        }

                        // other host has secret input; create dummy input gate
                        event.recv.id == ABY.SECRET_INPUT && event.recv.host != host -> {
                            secretInput = ABYDummyInGate(circuitType)
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
            storeValue.addConversionGates(circuitType)
        }
    }

    private suspend fun runSecretExpr(circuitType: ABYCircuitType, expr: PureExpressionNode): ABYCircuitGate {
        return when (expr) {
            is LiteralNode -> valueToCircuit(circuitType, expr.value)

            is ReadNode -> runSecretRead(circuitType, expr)

            is OperatorApplicationNode -> {
                val circuitArguments: List<ABYCircuitGate> =
                    expr.arguments.map { arg -> runSecretExpr(circuitType, arg) }
                operatorToCircuit(expr.operator, circuitArguments, circuitType)
            }

            is QueryNode -> {
                val loc = getObjectLocation(expr.variable.value)
                getObject(loc).query(circuitType, expr.query, expr.arguments)
            }

            is DeclassificationNode -> runSecretExpr(circuitType, expr.expression)
            is EndorsementNode -> runSecretExpr(circuitType, expr.expression)
        }
    }

    /** Return either a cleartext or secret-shared value. Used by array indexing. */
    private suspend fun runPlaintextOrSecretExpr(
        circuitType: ABYCircuitType,
        expr: AtomicExpressionNode
    ): ABYValue {
        return when (expr) {
            is LiteralNode -> ABYCleartextValue(expr.value)
            is ReadNode -> {
                val isCleartextRead =
                    protocolAnalysis
                        .relevantCommunicationEvents(expr)
                        .all { event -> event.recv.id == ABY.CLEARTEXT_INPUT }

                if (isCleartextRead) {
                    ABYCleartextValue(runPlaintextRead(expr))
                } else {
                    ABYSecretValue(runSecretRead(circuitType, expr))
                }
            }
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

        logger.info { "arith gates: ${circuitBuilder.arithCircuit.numGates} rounds: ${circuitBuilder.arithCircuit.maxDepth}" }
        logger.info { "bool gates: ${circuitBuilder.boolCircuit.numGates} rounds: ${circuitBuilder.boolCircuit.maxDepth}" }
        logger.info { "yao gates: ${circuitBuilder.yaoCircuit.numGates} rounds: ${circuitBuilder.yaoCircuit.maxDepth}" }

        return circuitBuilder.circuit(outGate.circuitType).putOUTGate(shareStack.peek()!!, outRole)
    }

    private fun executeABYCircuit(letNode: LetNode, receivingHosts: Set<Host>): Value? {
        val thisHostReceives = receivingHosts.contains(host)
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

        logger.info {
            "executed ABY circuit in ${execDuration}ms, sent output to $outRole\n" +
            "total gates: ${aby.totalGates}\n" +
            "total depth: ${aby.totalDepth}\n" +
            "total time: ${aby.getTiming(Phase.P_TOTAL)}\n" +
            "total sent/recv: ${aby.getSentData(Phase.P_TOTAL)} / ${aby.getReceivedData(Phase.P_TOTAL)}\n" +
            "network time: ${aby.getTiming(Phase.P_NETWORK)}\n" +
            "setup time: ${aby.getTiming(Phase.P_SETUP)}\n" +
            "setup sent/recv: ${aby.getSentData(Phase.P_SETUP)} / ${aby.getReceivedData(Phase.P_SETUP)}\n" +
            "online time: ${aby.getTiming(Phase.P_ONLINE)}\n" +
            "online sent/recv: ${aby.getSentData(Phase.P_ONLINE)} / ${aby.getReceivedData(Phase.P_ONLINE)}\n"
        }

        return if (thisHostReceives) {
            val result: Int = outShare.clearValue32.toInt()
            when (val msgType: ValueType = typeAnalysis.type(letNode)) {
                is BooleanType -> BooleanValue(result != 0)

                is IntegerType -> IntegerValue(result)

                else -> throw Exception("unknown type $msgType")
            }
        } else null
    }

    override suspend fun runGuard(protocol: Protocol, expr: AtomicExpressionNode): Value {
        throw ViaductInterpreterError("ABY: Cannot execute conditional guard")
    }

    override suspend fun runLet(protocol: Protocol, stmt: LetNode) {
        when (val rhs = stmt.value) {
            is ReceiveNode -> throw IllegalInternalCommunicationError(rhs)

            is InputNode -> throw ViaductInterpreterError("cannot perform I/O in non-Local protocol")

            is PureExpressionNode -> {
                val circuitType = protocolCircuitType[protocol.protocolName]!!
                val rhsCircuit = runSecretExpr(circuitType, rhs)
                ssTempStore = ssTempStore.put(stmt.temporary.value, rhsCircuit)

                // execute circuit and broadcast to other protocols
                val readers: List<SimpleStatementNode> =
                    protocolAnalysis
                        .directRemoteReaders(stmt)
                        .filter { reader ->
                            !availableProtocols.contains(
                                protocolAnalysis.primaryProtocol(reader)
                            )
                        }

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
                            events.filter { event ->
                                availableProtocols.contains(event.send.protocol) &&
                                    event.send.host == host
                            }

                        for (event in hostEvents) {
                            runtime.send(outValue, event)
                        }
                    }
                }
            }
        }
    }

    override suspend fun runUpdate(protocol: Protocol, stmt: UpdateNode) {
        getObject(getObjectLocation(stmt.variable.value))
            .update(protocolCircuitType[protocol.protocolName]!!, stmt.update, stmt.arguments)
    }

    override suspend fun runOutput(protocol: Protocol, stmt: OutputNode) {
        throw ViaductInterpreterError("cannot perform I/O in non-Local protocol")
    }

    abstract class ABYClassObject {
        abstract suspend fun query(
            circuitType: ABYCircuitType,
            query: QueryNameNode,
            arguments: List<AtomicExpressionNode>
        ): ABYCircuitGate

        abstract suspend fun update(
            circuitType: ABYCircuitType,
            update: UpdateNameNode,
            arguments: List<AtomicExpressionNode>
        )
    }

    class ABYImmutableCellObject(private var gate: ABYCircuitGate) : ABYClassObject() {
        override suspend fun query(
            circuitType: ABYCircuitType,
            query: QueryNameNode,
            arguments: List<AtomicExpressionNode>
        ): ABYCircuitGate {
            return when (query.value) {
                is Get -> gate

                else -> {
                    throw ViaductInterpreterError("ABY: unknown query for immutable cell", query)
                }
            }
        }

        override suspend fun update(
            circuitType: ABYCircuitType,
            update: UpdateNameNode,
            arguments: List<AtomicExpressionNode>
        ) {
            throw ViaductInterpreterError("ABY: unknown update for immutable cell", update)
        }
    }

    inner class ABYMutableCellObject(private var gate: ABYCircuitGate) : ABYClassObject() {
        override suspend fun query(
            circuitType: ABYCircuitType,
            query: QueryNameNode,
            arguments: List<AtomicExpressionNode>
        ): ABYCircuitGate {
            return when (query.value) {
                is Get -> gate

                else -> {
                    throw ViaductInterpreterError("ABY: unknown query for mutable cell", query)
                }
            }
        }

        override suspend fun update(
            circuitType: ABYCircuitType,
            update: UpdateNameNode,
            arguments: List<AtomicExpressionNode>
        ) {
            gate = when (update.value) {
                is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                    this@ABYProtocolInterpreter.runSecretExpr(circuitType, arguments[0])
                }

                is Modify -> {
                    val circuitArg: ABYCircuitGate = runSecretExpr(circuitType, arguments[0])
                    operatorToCircuit(update.value.operator, listOf(gate, circuitArg), circuitType)
                }

                else ->
                    throw ViaductInterpreterError("ABY: unknown update for mutable cell", update)
            }
        }
    }

    inner class ABYVectorObject(circuitType: ABYCircuitType, val size: Int, defaultValue: Value) : ABYClassObject() {
        private val gates: ArrayList<ABYCircuitGate> = ArrayList(size)

        init {
            for (i: Int in 0 until size) {
                gates.add(valueToCircuit(circuitType, defaultValue))
            }
        }

        override suspend fun query(
            circuitType: ABYCircuitType,
            query: QueryNameNode,
            arguments: List<AtomicExpressionNode>
        ): ABYCircuitGate {
            return when (query.value) {
                is Get -> {
                    when (val index: ABYValue = runPlaintextOrSecretExpr(circuitType, arguments[0])) {
                        is ABYCleartextValue -> {
                            gates[((index.value) as IntegerValue).value]
                        }

                        // secret indexing requires muxing the entire array
                        is ABYSecretValue -> {
                            // return 0 in case of indexing error
                            var currentShare: ABYCircuitGate = ABYConstantGate(0, circuitType)
                            for (i in size - 1 downTo 0) {
                                val guard: ABYCircuitGate =
                                    operatorToCircuit(
                                        EqualTo,
                                        listOf(index.value, ABYConstantGate(i, circuitType)),
                                        circuitType
                                    )

                                val mux: ABYCircuitGate =
                                    operatorToCircuit(
                                        Mux,
                                        listOf(guard, gates[i], currentShare),
                                        circuitType
                                    )

                                currentShare = mux
                            }

                            currentShare
                        }
                    }
                }

                else -> throw ViaductInterpreterError("ABY: unknown query ${query.value} for vector", query)
            }
        }

        override suspend fun update(
            circuitType: ABYCircuitType,
            update: UpdateNameNode,
            arguments: List<AtomicExpressionNode>
        ) {
            when (val index: ABYValue = runPlaintextOrSecretExpr(circuitType, arguments[0])) {
                is ABYCleartextValue -> {
                    val intIndex = (index.value as IntegerValue).value
                    gates[intIndex] = when (update.value) {
                        is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                            runSecretExpr(circuitType, arguments[1])
                        }

                        is Modify -> {
                            val circuitArg: ABYCircuitGate = runSecretExpr(circuitType, arguments[1])
                            operatorToCircuit(
                                update.value.operator,
                                listOf(gates[intIndex], circuitArg),
                                circuitType
                            )
                        }

                        else -> throw ViaductInterpreterError("ABY: unknown update ${update.value} for vector", update)
                    }
                }

                // mux all array values
                is ABYSecretValue -> {
                    val circuitArg: ABYCircuitGate = runSecretExpr(circuitType, arguments[1])
                    for (i in 0 until size) {
                        val rhs = when (update.value) {
                            is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> circuitArg

                            is Modify -> {
                                operatorToCircuit(
                                    update.value.operator,
                                    listOf(gates[i], circuitArg),
                                    circuitType
                                )
                            }

                            else -> throw ViaductInterpreterError(
                                "ABY: unknown update ${update.value} for vector",
                                update
                            )
                        }

                        val guard: ABYCircuitGate =
                            operatorToCircuit(
                                EqualTo,
                                listOf(index.value, ABYConstantGate(i, circuitType)),
                                circuitType
                            )

                        val mux: ABYCircuitGate =
                            operatorToCircuit(
                                Mux,
                                listOf(guard, rhs, gates[i]),
                                circuitType
                            )

                        gates[i] = mux
                    }
                }
            }
        }
    }

    object ABYNullObject : ABYClassObject() {
        override suspend fun query(
            circuitType: ABYCircuitType,
            query: QueryNameNode,
            arguments: List<AtomicExpressionNode>
        ): ABYCircuitGate {
            throw ViaductInterpreterError("ABY: unknown query ${query.value} for null object", query)
        }

        override suspend fun update(
            circuitType: ABYCircuitType,
            update: UpdateNameNode,
            arguments: List<AtomicExpressionNode>
        ) {
            throw ViaductInterpreterError("ABY: unknown update ${update.value} for null object", update)
        }
    }

    companion object : ProtocolBackend {
        private const val DEFAULT_PORT = 7766
        private const val BITLEN: Long = 32

        private val protocolCircuitType: Map<ProtocolName, ABYCircuitType> =
            mapOf(
                ArithABY.protocolName to ABYCircuitType.ARITH,
                BoolABY.protocolName to ABYCircuitType.BOOL,
                YaoABY.protocolName to ABYCircuitType.YAO
            )

        override fun buildProtocolInterpreters(
            host: Host,
            program: ProgramNode,
            protocols: Set<Protocol>,
            protocolAnalysis: ProtocolAnalysis,
            runtime: ViaductRuntime,
            connectionMap: Map<Host, HostAddress>
        ): Iterable<ProtocolInterpreter> {
            // this has to be sorted so all hosts agree on the port number to use!
            val abyProtocols = protocols.filterIsInstance<ABY>().sorted()

            var currentPort = DEFAULT_PORT
            val currentHostPairs: MutableSet<Pair<Host, Host>> = mutableSetOf()
            val createdInterpreters: MutableSet<ABYProtocolInterpreter> = mutableSetOf()

            for (abyProtocol in abyProtocols) {
                val hostPair = abyProtocol.server to abyProtocol.client
                if (!currentHostPairs.contains(hostPair)) {
                    val role = if (abyProtocol.server == host) Role.SERVER else Role.CLIENT
                    val otherHost = if (role == Role.CLIENT) abyProtocol.server else abyProtocol.client

                    createdInterpreters.add(
                        ABYProtocolInterpreter(
                            host,
                            otherHost,
                            role,
                            program,
                            protocolAnalysis,
                            runtime,
                            connectionMap
                        )
                    )
                    currentHostPairs.add(hostPair)
                    currentPort++
                }
            }

            return createdInterpreters
        }
    }
}
