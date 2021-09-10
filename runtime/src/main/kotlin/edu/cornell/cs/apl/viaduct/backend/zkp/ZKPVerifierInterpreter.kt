package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.backend.ObjectLocation
import edu.cornell.cs.apl.viaduct.backend.SingleProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.backend.WireGenerator
import edu.cornell.cs.apl.viaduct.backend.WireTerm
import edu.cornell.cs.apl.viaduct.backend.asString
import edu.cornell.cs.apl.viaduct.backend.wireName
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.libsnarkwrapper.libsnarkwrapper.mkByteBuf
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
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
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
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
    val runtime: ViaductProcessRuntime
) :
    SingleProtocolInterpreter<ZKPObject>(program, runtime.projection.protocol) {

    private val ensureInit = ZKPInit

    private val prover = (runtime.projection.protocol as ZKP).prover
    private val typeAnalysis = TypeAnalysis.get(program)

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
        arguments: List<AtomicExpressionNode>
    ): ZKPObject {
        return when (className) {
            ImmutableCell -> ZKPObject.ZKPImmutableCell(getAtomicExprWire(arguments[0]))
            MutableCell -> ZKPObject.ZKPMutableCell(getAtomicExprWire(arguments[0]))
            Vector -> {
                val length = runPlaintextExpr(arguments[0]) as IntegerValue
                ZKPObject.ZKPVectorObject(length.value, length.type.defaultValue, wireGenerator)
            }
            else -> throw Exception("unknown object")
        }
    }

    override fun getNullObject(): ZKPObject {
        return ZKPObject.ZKPNullObject
    }

    private suspend fun runQuery(obj: ZKPObject, query: QueryNameNode, args: List<AtomicExpressionNode>): WireTerm =
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
                val index = runPlaintextExpr(args[0]) as IntegerValue
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
            is ReceiveNode -> throw ViaductInterpreterError("impossible")
        }

    private fun runPlaintextExpr(expr: AtomicExpressionNode): Value =
        when (expr) {
            is LiteralNode -> expr.value
            is ReadNode -> tempStore[expr.temporary.value]!!
        }

    override suspend fun runLet(stmt: LetNode) {
        val w = getExprWire(stmt.value)
        wireStore = wireStore.put(stmt.temporary.value, w)
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        when (val o = getObject(getObjectLocation(stmt.variable.value))) {
            is ZKPObject.ZKPImmutableCell -> throw Exception("runtime error")
            is ZKPObject.ZKPMutableCell -> {
                when (val updateValue = stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        o.value = getAtomicExprWire(stmt.arguments[0])
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify -> {
                        val arg = getAtomicExprWire(stmt.arguments[0])
                        o.value = wireGenerator.mkOp(updateValue.operator, listOf(o.value, arg))
                    }
                    else ->
                        throw Exception("runtime error")
                }
            }
            is ZKPObject.ZKPVectorObject -> {
                val index = runPlaintextExpr(stmt.arguments[0]) as IntegerValue
                when (val updateValue = stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        o.gates[index.value] = getAtomicExprWire(stmt.arguments[1])
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify -> {
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
        events: ProtocolCommunication
    ) {
        val hostEvents = events.getHostSends(runtime.projection.host)
        if (sendProtocol != recvProtocol && hostEvents.isNotEmpty()) {
            val wire = wireStore[sender.temporary.value]!!
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
        events: ProtocolCommunication
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
                tempStore = tempStore.put(sender.temporary.value, cleartextValue!!)
                mkConst(cleartextValue)
            }
            wireStore = wireStore.put(sender.temporary.value, w)
        }
    }
}
