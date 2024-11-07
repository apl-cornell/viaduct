package io.github.aplcornell.viaduct.backend.zkp

import io.github.aplcornell.viaduct.analysis.ProtocolAnalysis
import io.github.aplcornell.viaduct.analysis.TypeAnalysis
import io.github.aplcornell.viaduct.backend.ObjectLocation
import io.github.aplcornell.viaduct.backend.SingleProtocolInterpreter
import io.github.aplcornell.viaduct.backend.ViaductProcessRuntime
import io.github.aplcornell.viaduct.backend.WireGenerator
import io.github.aplcornell.viaduct.backend.WireTerm
import io.github.aplcornell.viaduct.backend.asString
import io.github.aplcornell.viaduct.backend.commitment.genNonce
import io.github.aplcornell.viaduct.backend.eval
import io.github.aplcornell.viaduct.backend.wireName
import io.github.aplcornell.viaduct.backends.zkp.ZKP
import io.github.aplcornell.viaduct.errors.ViaductInterpreterError
import io.github.aplcornell.viaduct.libsnarkwrapper.libsnarkwrapper
import io.github.aplcornell.viaduct.libsnarkwrapper.libsnarkwrapper.mkByteBuf
import io.github.aplcornell.viaduct.selection.ProtocolCommunication
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolProjection
import io.github.aplcornell.viaduct.syntax.QueryNameNode
import io.github.aplcornell.viaduct.syntax.Temporary
import io.github.aplcornell.viaduct.syntax.datatypes.ClassName
import io.github.aplcornell.viaduct.syntax.datatypes.Get
import io.github.aplcornell.viaduct.syntax.datatypes.ImmutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.MutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.Vector
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclassificationNode
import io.github.aplcornell.viaduct.syntax.intermediate.EndorsementNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.InputNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.LiteralNode
import io.github.aplcornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutputNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.SimpleStatementNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.types.BooleanType
import io.github.aplcornell.viaduct.syntax.types.IntegerType
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.syntax.values.BooleanValue
import io.github.aplcornell.viaduct.syntax.values.ByteVecValue
import io.github.aplcornell.viaduct.syntax.values.IntegerValue
import io.github.aplcornell.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Stack

private val logger = KotlinLogging.logger("ZKP Prover")

class ZKPProverInterpreter(
    program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    val runtime: ViaductProcessRuntime,
) :
    SingleProtocolInterpreter<ZKPObject>(program, runtime.projection.protocol) {
    private val typeAnalysis = program.analyses.get<TypeAnalysis>()
    private val ensureInit = ZKPInit

    private val verifiers = (runtime.projection.protocol as ZKP).verifiers

    private val wireGenerator = WireGenerator()

    private val tempStack: Stack<PersistentMap<Temporary, Value>> = Stack()

    private var tempStore: PersistentMap<Temporary, Value>
        get() {
            return tempStack.peek()
        }
        set(value) {
            tempStack.pop()
            tempStack.push(value)
        }

    private val wireStack: Stack<PersistentMap<Temporary, WireTerm>> = Stack()

    private var wireStore: PersistentMap<Temporary, WireTerm>
        get() {
            return wireStack.peek()
        }
        set(value) {
            wireStack.pop()
            wireStack.push(value)
        }

    init {
        assert(runtime.projection.protocol is ZKP)

        objectStoreStack.push(persistentMapOf())
        wireStack.push(persistentMapOf())
        tempStack.push(persistentMapOf())
    }

    private fun pushContext(
        newObjectStore: PersistentMap<ObjectVariable, ObjectLocation>,
        newTempStore: PersistentMap<Temporary, Value>,
        newWireStore: PersistentMap<Temporary, WireTerm>,
    ) {
        objectStoreStack.push(newObjectStore)
        tempStack.push(newTempStore)
        wireStack.push(newWireStore)
    }

    override suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        pushContext(initialStore, persistentMapOf(), persistentMapOf())
    }

    override suspend fun pushContext() {
        pushContext(this.objectStore, this.tempStore, this.wireStore)
    }

    override suspend fun popContext() {
        tempStack.pop()
        wireStack.pop()
        objectStoreStack.pop()
    }

    private fun injectConst(value: Value): WireTerm {
        val i =
            when (value) {
                is IntegerValue -> value.value
                is BooleanValue ->
                    if (value.value) {
                        1
                    } else {
                        0
                    }

                else -> throw Exception("runtime error: unexpected value $value")
            }
        return wireGenerator.mkConst(i)
    }

    private suspend fun mkInput(value: Value): WireTerm {
        val i =
            when (value) {
                is IntegerValue -> value.value
                is BooleanValue ->
                    if (value.value) {
                        1
                    } else {
                        0
                    }

                else -> throw Exception("runtime error: unexpected value $value")
            }
        val nonce = genNonce(32) // 256 / 8 = 32

        val hash = libsnarkwrapper.get_sha_nonce_val(mkByteBuf(nonce.toByteArray()), i.toLong())

        for (h: Host in verifiers) {
            runtime.send(ByteVecValue(hash._data.toList()), ProtocolProjection(runtime.projection.protocol, h))
            runtime.send(ByteVecValue(nonce.toList()), ProtocolProjection(runtime.projection.protocol, h))
        }
        return wireGenerator.mkIn(i, hash._data.toList(), nonce.toList())
    }

    private fun Int.toValue(t: ValueType): Value {
        return when (t) {
            is BooleanType -> {
                when (this) {
                    0 -> BooleanValue(false)
                    1 -> BooleanValue(true)
                    else -> throw Error("toValue: cannot convert value $this to boolean")
                }
            }

            is IntegerType -> IntegerValue(this)
            else -> throw Exception("toValue: cannot convert type $t")
        }
    }

    override suspend fun buildExpressionObject(expr: AtomicExpressionNode): ZKPObject {
        return ZKPObject.ZKPImmutableCell(getAtomicExprWire(expr))
    }

    override suspend fun buildObject(
        className: ClassName,
        typeArguments: List<ValueType>,
        arguments: List<AtomicExpressionNode>,
    ): ZKPObject {
        return when (className) {
            ImmutableCell -> ZKPObject.ZKPImmutableCell(getAtomicExprWire(arguments[0]))
            MutableCell -> ZKPObject.ZKPMutableCell(getAtomicExprWire(arguments[0]))
            Vector -> {
                val length = runCleartextExpr(arguments[0]) as IntegerValue
                ZKPObject.ZKPVectorObject(length.value, length.type.defaultValue, wireGenerator)
            }

            else -> throw Exception("unknown object")
        }
    }

    override fun getNullObject(): ZKPObject {
        return ZKPObject.ZKPNullObject
    }

    private fun runQuery(
        obj: ZKPObject,
        query: QueryNameNode,
        args: List<AtomicExpressionNode>,
    ): WireTerm =
        when (obj) {
            is ZKPObject.ZKPImmutableCell ->
                if (query.value is Get) {
                    obj.value
                } else {
                    throw Exception("bad query")
                }

            is ZKPObject.ZKPMutableCell ->
                if (query.value is Get) {
                    obj.value
                } else {
                    throw Exception("bad query")
                }

            is ZKPObject.ZKPVectorObject ->
                if (query.value is Get) {
                    val index = runCleartextExpr(args[0]) as IntegerValue
                    obj.gates[index.value]
                } else {
                    throw Exception("bad query")
                }

            ZKPObject.ZKPNullObject -> throw Exception("null query")
        }

    private fun getAtomicExprWire(expr: AtomicExpressionNode): WireTerm =
        when (expr) {
            is LiteralNode -> injectConst(expr.value)
            is ReadNode -> wireStore[expr.temporary.value]!!
        }

    private suspend fun getExprWire(expr: ExpressionNode): WireTerm =
        when (expr) {
            is LiteralNode -> getAtomicExprWire(expr)
            is ReadNode -> getAtomicExprWire(expr)
            is OperatorApplicationNode -> {
                val args = expr.arguments.map { getExprWire(it) }
                wireGenerator.mkOp(expr.operator, args)
            }

            is QueryNode -> runQuery(getObject(getObjectLocation(expr.variable.value)), expr.query, expr.arguments)
            is DeclassificationNode -> getExprWire(expr.expression)
            is EndorsementNode -> getExprWire(expr.expression)
            is InputNode -> throw ViaductInterpreterError("impossible")
        }

    private fun runCleartextExpr(expr: AtomicExpressionNode): Value {
        return when (expr) {
            is LiteralNode -> expr.value
            is ReadNode -> tempStore[expr.temporary.value]!!
        }
    }

    override suspend fun runLet(stmt: LetNode) {
        logger.info {
            "running let for ${stmt.name.value}"
        }
        val w = getExprWire(stmt.value)
        wireStore = wireStore.put(stmt.name.value, w)
        logger.info {
            "Storing wire for ${stmt.name.value}"
        }
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        when (val o = getObject(getObjectLocation(stmt.variable.value))) {
            is ZKPObject.ZKPImmutableCell -> throw Exception("runtime error")
            is ZKPObject.ZKPMutableCell -> {
                when (val updateValue = stmt.update.value) {
                    is io.github.aplcornell.viaduct.syntax.datatypes.Set ->
                        o.value = getAtomicExprWire(stmt.arguments[0])

                    is io.github.aplcornell.viaduct.syntax.datatypes.Modify -> {
                        val arg = getAtomicExprWire(stmt.arguments[0])
                        o.value = wireGenerator.mkOp(updateValue.operator, listOf(o.value, arg))
                    }

                    else ->
                        throw Exception("runtime error")
                }
            }

            is ZKPObject.ZKPVectorObject -> {
                val index = runCleartextExpr(stmt.arguments[0]) as IntegerValue
                when (val updateValue = stmt.update.value) {
                    is io.github.aplcornell.viaduct.syntax.datatypes.Set ->
                        o.gates[index.value] = getAtomicExprWire(stmt.arguments[1])

                    is io.github.aplcornell.viaduct.syntax.datatypes.Modify -> {
                        val arg = getAtomicExprWire(stmt.arguments[1])
                        o.gates[index.value] =
                            wireGenerator.mkOp(updateValue.operator, listOf(o.gates[index.value], arg))
                    }

                    else ->
                        throw Exception("runtime error")
                }
            }

            ZKPObject.ZKPNullObject -> throw Exception("runtime error")
        }
    }

    override suspend fun runOutput(stmt: OutputNode) {
        throw Exception("cannot perform I/O in non-Local protocol")
    }

    override suspend fun runGuard(expr: AtomicExpressionNode): Value {
        throw ViaductInterpreterError("ZKP: Cannot execute conditional guard")
    }

    override suspend fun runSend(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication,
    ) {
        if (sendProtocol != recvProtocol) {
            val wire = wireStore[sender.name.value]!!
            val wireVal: Int = wire.eval()
            logger.info {
                "Run let on wire $wire with output value $wireVal"
            }
            val wireName = wire.wireName()
            logger.info {
                "Wire name = $wireName"
            }
            val r1cs = wire.toR1CS(true, wireVal)

            withContext(Dispatchers.IO) {
                val pkFile = File("zkpkeys/$wireName.pk")
                if (!pkFile.exists()) { // Create proving key, and abort
                    pkFile.createNewFile()
                    val vkFile = File("zkpkeys/$wireName.vk")
                    vkFile.createNewFile()
                    val kp = r1cs.genKeypair()
                    val outPkFile = FileOutputStream(pkFile, false)
                    logger.info { "kp : writing ${kp.proving_key._data.size} bytes" }
                    outPkFile.write(kp.proving_key._data)
                    logger.info { "vk : writing ${kp.verification_key._data.size} bytes" }
                    val outVkFile = FileOutputStream(vkFile, false)
                    outVkFile.write(kp.verification_key._data)
                    outPkFile.close()
                    outVkFile.close()
                    throw Exception(
                        "Created new proving key and verification key for wire ${wire.asString()} with name $wireName. Rerun to use.",
                    )
                } else { // Read proving key, make proof, send to all the verifiers
                    val inPkFile = FileInputStream(pkFile)
                    val pk = mkByteBuf(inPkFile.readAllBytes())
                    inPkFile.close()
                    logger.info { "Proving.." }
                    val pf = r1cs.makeProof(pk)
                    logger.info { "Proof done!" }

                    // send to all verifiers who need to release output
                    for (v: Host in verifiers) {
                        if (events.getHostSends(v).isNotEmpty()) {
                            val hostProjection = ProtocolProjection(runtime.projection.protocol, v)
                            runtime.send(IntegerValue(wireVal), hostProjection)
                            runtime.send(ByteVecValue(pf._data.toList()), hostProjection)
                        }
                    }
                }
            }

            val hostEvents = events.getHostSends(runtime.projection.host)
            for (event in hostEvents) {
                runtime.send(
                    wireVal.toValue(typeAnalysis.type(sender)),
                    ProtocolProjection(event.recv.protocol, event.recv.host),
                )
            }
        }
    }

    override suspend fun runReceive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication,
    ) {
        if (sendProtocol != recvProtocol) {
            logger.info {
                val senderName = sender.name.value
                val runtimeProtocol = runtime.projection.protocol
                "Wire for $senderName does not exist; sendProtocol = $sendProtocol, runtimeProtocol = $runtimeProtocol"
            }
            val secretInputs = events.getHostReceives(runtime.projection.host, "ZKP_SECRET_INPUT")
            val publicInputs = events.getHostReceives(runtime.projection.host, "ZKP_PUBLIC_INPUT")
            when {
                secretInputs.isNotEmpty() && publicInputs.isEmpty() -> {
                    assert(secretInputs.size == 1)
                    val sendEvent = secretInputs.first()
                    val msg = runtime.receive(ProtocolProjection(sendEvent.send.protocol, sendEvent.send.host))
                    val wire = mkInput(msg)
                    tempStore = tempStore.put(sender.name.value, msg)
                    wireStore = wireStore.put(sender.name.value, wire)
                }

                secretInputs.isEmpty() && publicInputs.isNotEmpty() -> {
                    var cleartextValue: Value? = null
                    for (event in publicInputs) {
                        val receivedValue: Value =
                            runtime.receive(ProtocolProjection(event.send.protocol, event.send.host))

                        if (cleartextValue == null) {
                            cleartextValue = receivedValue
                        } else if (cleartextValue != receivedValue) {
                            throw ViaductInterpreterError("ZKP public input: received different values")
                        }
                    }
                    val wire = injectConst(cleartextValue!!)
                    tempStore = tempStore.put(sender.name.value, cleartextValue)
                    wireStore = wireStore.put(sender.name.value, wire)
                }

                else -> throw ViaductInterpreterError("Got weird ZKP situation: secret = $secretInputs, public = $publicInputs")
            }
        }
    }
}
