package io.github.aplcornell.viaduct.backend.zkp

import io.github.aplcornell.viaduct.analysis.ProtocolAnalysis
import io.github.aplcornell.viaduct.analysis.TypeAnalysis
import io.github.aplcornell.viaduct.backend.ObjectLocation
import io.github.aplcornell.viaduct.backend.SingleProtocolInterpreter
import io.github.aplcornell.viaduct.backend.ViaductProcessRuntime
import io.github.aplcornell.viaduct.backend.WireGenerator
import io.github.aplcornell.viaduct.backend.WireTerm
import io.github.aplcornell.viaduct.backend.asString
import io.github.aplcornell.viaduct.backend.wireName
import io.github.aplcornell.viaduct.backends.zkp.ZKP
import io.github.aplcornell.viaduct.errors.ViaductInterpreterError
import io.github.aplcornell.viaduct.libsnarkwrapper.libsnarkwrapper.mkByteBuf
import io.github.aplcornell.viaduct.selection.ProtocolCommunication
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
import mu.KotlinLogging
import java.io.File
import java.io.FileInputStream
import java.util.Stack

private val logger = KotlinLogging.logger("ZKP Verifier")

class ZKPVerifierInterpreter(
    program: ProgramNode,
    val protocolAnalysis: ProtocolAnalysis,
    val runtime: ViaductProcessRuntime,
) :
    SingleProtocolInterpreter<ZKPObject>(program, runtime.projection.protocol) {

    private val ensureInit = ZKPInit

    private val prover = (runtime.projection.protocol as ZKP).prover
    private val typeAnalysis = program.analyses.get<TypeAnalysis>()

    private val tempStack: Stack<PersistentMap<Temporary, Value>> = Stack()

    private var tempStore: PersistentMap<Temporary, Value>
        get() {
            return tempStack.peek()
        }
        set(value) {
            tempStack.pop()
            tempStack.push(value)
        }

    private val wireGenerator = WireGenerator()

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
        tempStack.push(persistentMapOf())
        wireStack.push(persistentMapOf())
    }

    override suspend fun pushContext() {
        pushContext(objectStore)
    }

    override suspend fun pushContext(initialStore: PersistentMap<ObjectVariable, ObjectLocation>) {
        objectStoreStack.push(initialStore)
        tempStack.push(persistentMapOf())
        wireStack.push(persistentMapOf())
    }

    override suspend fun popContext() {
        tempStack.pop()
        objectStoreStack.pop()
        wireStack.pop()
    }

    private fun Value.toInt(): Int {
        return when (this) {
            is IntegerValue -> this.value
            is BooleanValue -> if (this.value) {
                1
            } else {
                0
            }

            else -> throw Exception("value.toInt: Unknown value type: $this")
        }
    }

    /** Inject a value into a wire. **/
    private fun mkConst(value: Value): WireTerm =
        wireGenerator.mkConst(value.toInt())

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

    private suspend fun mkDummyIn(): WireTerm {
        val hash = (runtime.receive(ProtocolProjection(runtime.projection.protocol, prover)) as ByteVecValue)
        val nonce = (runtime.receive(ProtocolProjection(runtime.projection.protocol, prover)) as ByteVecValue)
        return wireGenerator.mkDummyIn(hash.value, nonce.value)
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

    private fun runQuery(obj: ZKPObject, query: QueryNameNode, args: List<AtomicExpressionNode>): WireTerm =
        when (obj) {
            is ZKPObject.ZKPImmutableCell -> if (query.value is Get) {
                obj.value
            } else {
                throw Exception("bad query")
            }

            is ZKPObject.ZKPMutableCell -> if (query.value is Get) {
                obj.value
            } else {
                throw Exception("bad query")
            }

            is ZKPObject.ZKPVectorObject -> if (query.value is Get) {
                val index = runCleartextExpr(args[0]) as IntegerValue
                obj.gates[index.value]
            } else {
                throw Exception("bad query")
            }

            ZKPObject.ZKPNullObject -> throw Exception("null query")
        }

    private fun getAtomicExprWire(expr: AtomicExpressionNode): WireTerm =
        when (expr) {
            is LiteralNode -> mkConst(expr.value)
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

    private fun runCleartextExpr(expr: AtomicExpressionNode): Value =
        when (expr) {
            is LiteralNode -> expr.value
            is ReadNode -> tempStore[expr.temporary.value]!!
        }

    override suspend fun runLet(stmt: LetNode) {
        val w = getExprWire(stmt.value)
        wireStore = wireStore.put(stmt.name.value, w)
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
        throw ViaductInterpreterError("ZKP: Cannot run cleartext guard")
    }

    override suspend fun runSend(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication,
    ) {
        val hostEvents = events.getHostSends(runtime.projection.host)
        if (sendProtocol != recvProtocol && hostEvents.isNotEmpty()) {
            val wire = wireStore[sender.name.value]!!
            val wireName = wire.wireName()
            val vkFile = File("zkpkeys/$wireName.vk")
            if (!vkFile.exists()) {
                throw Exception("Cannot find verification key for ${wire.asString()} with name $wireName.vk.  Restart after prover finishes.")
            } else {
                val wireVal: Int =
                    (runtime.receive(ProtocolProjection(runtime.projection.protocol, prover)) as IntegerValue).value
                val r1cs = wire.toR1CS(false, wireVal)
                val pf =
                    (runtime.receive(ProtocolProjection(runtime.projection.protocol, prover)) as ByteVecValue).value
                val in_vkFile = FileInputStream(vkFile)
                val vk = mkByteBuf(in_vkFile.readAllBytes())
                in_vkFile.close()
                logger.info {
                    "Verifying.."
                }
                val verifyResult = r1cs.verifyProof(vk, mkByteBuf(pf.toByteArray()))
                logger.info {
                    "Verified: $verifyResult"
                }
                assert(verifyResult)

                for (event in hostEvents) {
                    val outVal = wireVal.toValue(typeAnalysis.type(sender))
                    runtime.send(outVal, ProtocolProjection(event.recv.protocol, event.recv.host))
                }
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
            val publicInputs = events.getHostReceives(runtime.projection.host, "ZKP_PUBLIC_INPUT")

            val w: WireTerm = if (publicInputs.isEmpty()) {
                mkDummyIn()
            } else {
                // Only kind of input is zkp public input
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
                tempStore = tempStore.put(sender.name.value, cleartextValue!!)
                mkConst(cleartextValue)
            }
            wireStore = wireStore.put(sender.name.value, w)
        }
    }
}
