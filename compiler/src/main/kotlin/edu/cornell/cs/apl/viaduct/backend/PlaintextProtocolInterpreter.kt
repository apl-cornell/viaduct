package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.commitment.HashInfo
import edu.cornell.cs.apl.viaduct.backend.commitment.encode
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Plaintext
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Plaintext")

class PlaintextProtocolInterpreter(
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val runtime: ViaductProcessRuntime
) : SingleProtocolInterpreter<PlaintextClassObject>(program, runtime.projection.protocol) {
    private val tempStoreStack: Stack<PersistentMap<Temporary, Value>> = Stack()

    private var tempStore: PersistentMap<Temporary, Value>
        get() {
            return tempStoreStack.peek()
        }
        set(value) {
            tempStoreStack.pop()
            tempStoreStack.push(value)
        }

    init {
        assert(runtime.projection.protocol is Local || runtime.projection.protocol is Replication)

        objectStoreStack.push(persistentMapOf())
        tempStoreStack.push(persistentMapOf())
    }

    private fun pushContext(
        newObjectStore: PersistentMap<ObjectVariable, ObjectLocation>,
        newTempStore: PersistentMap<Temporary, Value>
    ) {
        objectStoreStack.push(newObjectStore)
        tempStoreStack.push(newTempStore)
    }

    override suspend fun pushContext() {
        pushContext(this.objectStore, this.tempStore)
    }

    override suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf())
    }

    override suspend fun popContext() {
        objectStoreStack.pop()
        tempStoreStack.pop()
    }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): PlaintextClassObject {
        return ImmutableCellObject(runExpr(expr))
    }

    override suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>
    ): PlaintextClassObject {
        return when (className) {
            ImmutableCell -> ImmutableCellObject(runExpr(arguments[0]))

            MutableCell -> MutableCellObject(runExpr(arguments[0]))

            Vector -> {
                val length = runExpr(arguments[0]) as IntegerValue
                VectorObject(length.value, length.type.defaultValue)
            }

            else -> throw Exception("runtime error")
        }
    }

    override fun getNullObject(): PlaintextClassObject = NullObject

    private suspend fun runRead(read: ReadNode): Value {
        val storeValue = tempStore[read.temporary.value]
        return if (storeValue == null) {
            val sendProtocol = protocolAnalysis.primaryProtocol(read)

            // must receive from read protocol
            if (runtime.projection.protocol != sendProtocol) {
                val events: ProtocolCommunication = protocolAnalysis.relevantCommunicationEvents(read)

                val cleartextInputs =
                    events.getProjectionReceives(runtime.projection, Plaintext.INPUT)

                val cleartextCommitmentInputs =
                    events.getProjectionReceives(runtime.projection, Plaintext.CLEARTEXT_COMMITMENT_INPUT)

                val hashCommitmentInputs =
                    events.getProjectionReceives(runtime.projection, Plaintext.HASH_COMMITMENT_INPUT)

                return when {
                    // cleartext input
                    cleartextInputs.isNotEmpty() && cleartextCommitmentInputs.isEmpty() && hashCommitmentInputs.isEmpty() -> {
                        var cleartextValue: Value? = null
                        for (event in cleartextInputs) {
                            val receivedValue: Value = runtime.receive(event)

                            if (cleartextValue == null) {
                                cleartextValue = receivedValue
                            } else if (cleartextValue != receivedValue) {
                                throw ViaductInterpreterError("Plaintext: received different values")
                            }
                        }

                        assert(cleartextValue != null)
                        tempStore = tempStore.put(read.temporary.value, cleartextValue!!)
                        cleartextValue
                    }

                    // commitment opening
                    cleartextInputs.isEmpty() && cleartextCommitmentInputs.isNotEmpty() && hashCommitmentInputs.isNotEmpty() -> {
                        assert(cleartextCommitmentInputs.size == 1)
                        val cleartextSendEvent = cleartextCommitmentInputs.first()

                        val cleartextProjection = cleartextSendEvent.send.asProjection()
                        val nonce = runtime.receive(cleartextProjection) as ByteVecValue
                        val msg = runtime.receive(cleartextProjection)

                        logger.info {
                            "received opened commitment value and nonce from ${cleartextProjection.asDocument.print()}"
                        }

                        for (hashCommitmentInput in hashCommitmentInputs) {
                            val commitmentSender =
                                ProtocolProjection(
                                    hashCommitmentInput.send.protocol,
                                    hashCommitmentInput.send.host
                                )

                            val commitment: ByteVecValue = runtime.receive(commitmentSender) as ByteVecValue

                            assert(HashInfo(commitment.value, nonce.value).verify(msg.encode()))
                            logger.info {
                                "verified commitment from host ${commitmentSender.asDocument.print()}"
                            }
                        }

                        tempStore = tempStore.put(read.temporary.value, msg)
                        msg
                    }

                    else ->
                        throw ViaductInterpreterError("Plaintext: received both commitment opening and cleartext value")
                }
            } else { // temporary should be stored locally, but isn't
                throw ViaductInterpreterError("Plaintext: could not find local temporary ${read.temporary.value}")
            }
        } else {
            storeValue
        }
    }

    suspend fun runExpr(expr: ExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value

            is ReadNode -> runRead(expr)

            is OperatorApplicationNode -> {
                val argValues = expr.arguments.map { runExpr(it) }
                expr.operator.apply(argValues)
            }

            is QueryNode -> {
                objectStore[expr.variable.value]?.let { loc ->
                    val argValues: List<Value> = expr.arguments.map { arg -> runExpr(arg) }
                    objectHeap[loc].query(expr.query, argValues)
                } ?: throw UndefinedNameError(expr.variable)
            }

            is DowngradeNode -> runExpr(expr.expression)

            is InputNode -> {
                when (runtime.projection.protocol) {
                    is Local -> runtime.input()

                    else -> throw ViaductInterpreterError("Cannot perform I/O in non-Local protocol", expr)
                }
            }

            is ReceiveNode -> TODO()
        }
    }

    override suspend fun runGuard(expr: AtomicExpressionNode): Value = runExpr(expr)

    override suspend fun runLet(stmt: LetNode) {
        val rhsValue = runExpr(stmt.value)
        tempStore = tempStore.put(stmt.temporary.value, rhsValue)

        // broadcast to readers
        val readers: Set<SimpleStatementNode> = protocolAnalysis.directRemoteReaders(stmt)

        for (reader in readers) {
            val events =
                protocolAnalysis
                    .relevantCommunicationEvents(stmt, reader)
                    .getProjectionSends(runtime.projection)

            for (event in events) {
                runtime.send(rhsValue, event)
            }
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        val argValues: List<Value> = stmt.arguments.map { arg -> runExpr(arg) }
        getObject(getObjectLocation(stmt.variable.value)).update(stmt.update, argValues)
    }

    override suspend fun runOutput(stmt: OutputNode) {
        when (runtime.projection.protocol) {
            is Local -> {
                val outputValue = runExpr(stmt.message)
                runtime.output(outputValue)
            }

            else -> throw ViaductInterpreterError("Cannot perform I/O in non-Local protocol", stmt)
        }
    }

    companion object : ProtocolBackend {
        override fun buildProtocolInterpreters(
            host: Host,
            program: ProgramNode,
            protocols: Set<Protocol>,
            protocolAnalysis: ProtocolAnalysis,
            runtime: ViaductRuntime,
            connectionMap: Map<Host, HostAddress>
        ): Iterable<ProtocolInterpreter> {
            return protocols.filter { protocol ->
                protocol.protocolName == Local.protocolName ||
                    protocol.protocolName == Replication.protocolName
            }.map { protocol ->
                PlaintextProtocolInterpreter(
                    program,
                    protocolAnalysis,
                    ViaductProcessRuntime(runtime, ProtocolProjection(protocol, host))
                )
            }
        }
    }
}
