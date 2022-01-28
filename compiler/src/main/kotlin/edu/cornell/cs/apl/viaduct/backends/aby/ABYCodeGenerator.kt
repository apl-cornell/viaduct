package edu.cornell.cs.apl.viaduct.backends.aby

import com.github.apl_cornell.aby.ABYParty
import com.github.apl_cornell.aby.Role
import com.github.apl_cornell.aby.SharingType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.sun.jdi.IntegerValue
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.codegeneration.AbstractCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.getRole
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.PureExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Negation
import edu.cornell.cs.apl.viaduct.syntax.operators.Subtraction
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

private const val ABYPORT = 8000

private data class ABYPair(val server: Host, val client: Host)

class ABYCodeGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    private val typeAnalysis: TypeAnalysis = TypeAnalysis.get(context.program)
    private val nameAnalysis: NameAnalysis = NameAnalysis.get(context.program)
    private val protocolAnalysis: ProtocolAnalysis = ProtocolAnalysis(context.program, context.protocolComposer)
    private var protocolToABYPartyMap: MutableMap<ABYPair, String> = mutableMapOf()

    override fun setup(protocol: Protocol): List<PropertySpec> =
        listOf(
            PropertySpec.builder(
                protocolToABYPartyMap.getOrPut(
                    ABYPair((protocol as ABY).server, protocol.client),
                    { context.newTemporary("abyParty") }
                ),
                ABYParty::class
            ).initializer(
                "ABYParty(%L, %L.getHost(), %L, %L, %L)",
                getRole(protocol, context.host),
                context.url(context.host), // TODO() - check whether client and server put in same IP address
                ABYPORT,
                "Aby.getLT()", // TODO() - how to make this not hard-coded
                32 // TODO() - where is best place to store this value?
            )
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

    private val bitLen = 32

    private fun addConversionGates(
        destProtocol: Protocol,
        sourceProtocol: Protocol,
        kotlinName: String,
        circuitBuilder: CodeBlock
    ): CodeBlock =
        when (protocolToShareType(destProtocol)) {
            SharingType.S_YAO -> {
                when (protocolToShareType(sourceProtocol)) {
                    SharingType.S_YAO -> CodeBlock.of("%L", "")
                    SharingType.S_BOOL -> CodeBlock.of(".putY2BGate(%L)", kotlinName)
                    SharingType.S_ARITH -> CodeBlock.of(".putB2AGate(%L.putY2BGate(%L))", circuitBuilder, kotlinName)
                    else -> throw CodeGenerationError(
                        "unsupported ABY protocol: ${sourceProtocol.toDocument().print()}"
                    )
                }
            }
            SharingType.S_BOOL -> {
                when (protocolToShareType(sourceProtocol)) {
                    SharingType.S_YAO -> CodeBlock.of(".putB2YGate(%L)", kotlinName)
                    SharingType.S_BOOL -> CodeBlock.of("%L", "")
                    SharingType.S_ARITH -> CodeBlock.of(".putB2AGate(%L)", kotlinName)
                    else -> throw CodeGenerationError(
                        "unsupported ABY protocol: ${sourceProtocol.toDocument().print()}"
                    )
                }
            }
            SharingType.S_ARITH -> {
                when (protocolToShareType(sourceProtocol)) {
                    SharingType.S_YAO -> CodeBlock.of(".putA2YGate(%L)", kotlinName)
                    SharingType.S_BOOL -> CodeBlock.of(".putY2BGate(%L.putA2YGate(%L))")
                    SharingType.S_ARITH -> CodeBlock.of("%L", "")
                    else -> throw CodeGenerationError(
                        "unsupported ABY protocol: ${sourceProtocol.toDocument().print()}"
                    )
                }
            }
            else ->
                throw CodeGenerationError("unsupported ABY protocol: ${destProtocol.toDocument().print()}")
        }

    private fun shareTypeToCodeBlock(shareType: SharingType): CodeBlock =
        when (shareType) {
            SharingType.S_ARITH -> CodeBlock.of("%L", "SharingType.S_ARITH")
            SharingType.S_BOOL -> CodeBlock.of("%L", "SharingType.S_BOOL")
            SharingType.S_YAO -> CodeBlock.of("%L", "SharingType.S_YAO")
            else -> throw CodeGenerationError("unknown share type: $shareType")
        }

    private fun protocolToAbyPartyCircuit(protocol: Protocol): CodeBlock =
        CodeBlock.of(
            "%L.getCircuitBuilder(%L)",
            protocolToABYPartyMap.getValue(ABYPair((protocol as ABY).server, protocol.client)),
            shareTypeToCodeBlock(protocolToShareType(protocol))
        )

    private fun protocolToShareType(protocol: Protocol): SharingType =
        when (protocol) {
            is ArithABY -> SharingType.S_ARITH
            is BoolABY -> SharingType.S_BOOL
            is YaoABY -> SharingType.S_YAO
            else -> throw CodeGenerationError("unknown ABY protocol: ${protocol.toDocument().print()}")
        }

    private fun valueToShare(value: Value, protocol: Protocol): CodeBlock =
        when (value) {
            is BooleanValue ->
                CodeBlock.of(
                    "%L.putCONSGate(%L.toInt().toBigInteger(), %L)",
                    protocolToAbyPartyCircuit(protocol),
                    value.value,
                    bitLen
                )
            is IntegerValue ->
                CodeBlock.of(
                    "%L.putCONSGate(%L, %L)",
                    protocolToAbyPartyCircuit(protocol),
                    value.value(),
                    bitLen
                )
            else -> throw CodeGenerationError("unknown value type: ${value.toDocument().print()}")
        }

    private fun binaryOpToShare(
        circuit: CodeBlock,
        gateMethod: CodeBlock,
        arg1: CodeBlock,
        arg2: CodeBlock
    ): CodeBlock =
        CodeBlock.of("%L.%L(%L, %L)", circuit, gateMethod, arg1, arg2)

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> valueToShare(expr.value, protocol)

            is ReadNode ->
                CodeBlock.of(
                    "%L%L",
                    protocolToAbyPartyCircuit(protocol),
                    addConversionGates(
                        protocol, // destination protocol
                        context.tempKotlinNameToProtocol(
                            context.kotlinName(
                                expr.temporary.value,
                                protocol
                            )
                        ), // source protocol
                        context.kotlinName(expr.temporary.value, protocol), // share name
                        protocolToAbyPartyCircuit(protocol) // circuit builder
                    )
                )

            is OperatorApplicationNode -> {
                when (expr.operator) {
                    Minimum ->
                        CodeBlock.of(
                            "putMinGate(%L, %L, %L)",
                            protocolToAbyPartyCircuit(protocol),
                            exp(protocol, expr.arguments[0]),
                            exp(protocol, expr.arguments[1])
                        )

                    Maximum ->
                        CodeBlock.of(
                            "putMaxGate(%L, %L, %L)",
                            protocolToAbyPartyCircuit(protocol),
                            exp(protocol, expr.arguments[0]),
                            exp(protocol, expr.arguments[1])
                        )

                    Negation -> TODO()

                    Addition ->
                        binaryOpToShare(
                            protocolToAbyPartyCircuit(protocol),
                            CodeBlock.of("putADDGate"),
                            exp(protocol, expr.arguments[0]),
                            exp(protocol, expr.arguments[1])
                        )

                    Subtraction ->
                        binaryOpToShare(
                            protocolToAbyPartyCircuit(protocol),
                            CodeBlock.of("putSUBGate"),
                            exp(protocol, expr.arguments[1]),
                            exp(protocol, expr.arguments[0])
                        )

                    Multiplication ->
                        binaryOpToShare(
                            protocolToAbyPartyCircuit(protocol),
                            CodeBlock.of("putMULGate"),
                            exp(protocol, expr.arguments[0]),
                            exp(protocol, expr.arguments[1])
                        )

                    else -> throw CodeGenerationError(
                        "unknown operator: ${
                        expr.operator.toDocument(expr.arguments).print()
                        }"
                    )
                }
            }

            is QueryNode -> TODO()
/*                when(typeAnalysis.type(nameAnalysis.declaration(expr))) {
                    is VectorType -> {}
                    is ImmutableCellType -> expr.arguments[0]
                    is MutableCellType -> {}
                }*/

            is DeclassificationNode -> exp(protocol, expr.expression)

            is EndorsementNode -> exp(protocol, expr.expression)

            is DowngradeNode -> exp(protocol, expr.expression)

            is InputNode ->
                throw CodeGenerationError("cannot perform I/O in non-Local protocol")
        }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        when (stmt.value) {
            is InputNode -> throw CodeGenerationError("cannot perform I/O in non-Local protocol")
            is PureExpressionNode -> {
                CodeBlock.of(
                    "val %N = %L",
                    context.kotlinName(stmt.temporary.value, protocol),
                    exp(protocol, stmt.value)
                )
            }
        }

    private fun isSecretArgument(arg: AtomicExpressionNode): Boolean =
        when (arg) {
            is LiteralNode -> true
            is ReadNode -> protocolAnalysis.relevantCommunicationEvents(arg)
                .all { event -> event.recv.id == ABY.CLEARTEXT_INPUT }
        }

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock {
        val updateBuilder = CodeBlock.builder()
        when (typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is VectorType -> {
                if (isSecretArgument(stmt.arguments[0])) {
                    // TODO() - ask about muxing
                } else {
                    exp(protocol, stmt.arguments[1]) // TODO() - check this
                }
            }

            is MutableCellType -> exp(protocol, stmt.arguments[0])

            is ImmutableCellType ->
                throw CodeGenerationError("ABY: unknown update for immutable cell", stmt)
        }

        return updateBuilder.build()
    }

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        throw CodeGenerationError("cannot perform I/O in non-local protocol")

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        throw CodeGenerationError("ABY: Cannot execute conditional guard")

    private fun roleToCodeBlock(role: Role): CodeBlock =
        when (role) {
            Role.CLIENT -> CodeBlock.of("Role.CLIENT")
            Role.SERVER -> CodeBlock.of("Role.Server")
            Role.ALL -> CodeBlock.of("Role.ALL")
        }

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val outBuilder = CodeBlock.builder()

        val otherHostInfo: Pair<Host, Role> =
            if (context.host == (sendProtocol as ABY).client) {
                Pair(sendProtocol.server, Role.SERVER)
            } else {
                Pair(sendProtocol.client, Role.CLIENT)
            }

        val thisHostRole = if (otherHostInfo.second == Role.CLIENT) Role.SERVER else Role.CLIENT

        val receivingHosts = events.map { event -> event.recv.host }.toSet()
        val thisHostReceives = receivingHosts.contains(context.host)
        val otherHostReceives = receivingHosts.contains(otherHostInfo.first)
        val outRole: CodeBlock =
            when {
                thisHostReceives && !otherHostReceives -> roleToCodeBlock(thisHostRole)

                !thisHostReceives && otherHostReceives ->
                    if (thisHostRole == Role.SERVER) roleToCodeBlock(Role.CLIENT) else roleToCodeBlock(Role.SERVER)

                thisHostReceives && otherHostReceives -> roleToCodeBlock(Role.ALL)

                else ->
                    throw ViaductInterpreterError("ABY: at least one party must receive output when executing circuit")
            }

        // execute circuit
        outBuilder.addStatement("%L.execCircuit()", protocolToAbyPartyCircuit(sendProtocol))

        outBuilder.addStatement(
            "val %L = %L.putOUTGate(%L, %L).getClearValue32()",
            context.kotlinName(sender.temporary.value, receiveProtocol),
            protocolToAbyPartyCircuit(sendProtocol),
            context.kotlinName(sender.temporary.value, sendProtocol),
            outRole
        )

        val hostEvents = events.filter { event -> event.send.host == context.host }
        for (event in hostEvents) {
            outBuilder.add(
                context.send(
                    CodeBlock.of("%L", context.kotlinName(sender.temporary.value, receiveProtocol)),
                    event.recv.host
                )
            )
        }

        // TODO() - do I call ABY reset here?

        return outBuilder.build()
    }

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val receiveBuilder = CodeBlock.builder()
        for (event in events) {
            when {

                // secret input for this host; create input gate
                event.recv.id == ABY.SECRET_INPUT && event.recv.host == context.host -> {
                    when (typeAnalysis.type(sender)) {
                        is BooleanType ->
                            receiveBuilder.add(
                                CodeBlock.of(
                                    "%L.putINGate(%L.toInt().toBigInteger(), %L, %L)",
                                    protocolToAbyPartyCircuit(receiveProtocol),
                                    context.kotlinName(sender.temporary.value, sendProtocol),
                                    bitLen,
                                    getRole(receiveProtocol, context.host)
                                )
                            )

                        is IntegerType ->
                            receiveBuilder.add(
                                CodeBlock.of(
                                    "%L.putINGate(%L, %L, %L)",
                                    protocolToAbyPartyCircuit(receiveProtocol),
                                    context.kotlinName(sender.temporary.value, sendProtocol),
                                    bitLen,
                                    getRole(receiveProtocol, context.host)
                                )
                            )
                    }
                }

                // other host has secret input; create dummy gate
                event.recv.id == ABY.SECRET_INPUT && event.recv.host != context.host -> {
                    receiveBuilder.add(
                        CodeBlock.of(
                            "%L.putDummyINGate(%L)",
                            protocolToAbyPartyCircuit(receiveProtocol),
                            bitLen
                        )
                    )
                }

                // cleartext input; create constant gate
                event.recv.id == ABY.CLEARTEXT_INPUT && event.recv.host == context.host -> {
                    receiveBuilder.add(exp(sendProtocol, sender.value))
                }
            }
        }

        return receiveBuilder.build()
    }
}
