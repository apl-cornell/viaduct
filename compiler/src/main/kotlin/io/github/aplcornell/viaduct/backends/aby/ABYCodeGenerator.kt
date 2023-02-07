package io.github.aplcornell.viaduct.backends.aby

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.github.apl_cornell.aby.ABYParty
import io.github.apl_cornell.aby.Aby
import io.github.apl_cornell.aby.Role
import io.github.apl_cornell.aby.Share
import io.github.apl_cornell.aby.SharingType
import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.analysis.ProtocolAnalysis
import io.github.aplcornell.viaduct.analysis.TypeAnalysis
import io.github.aplcornell.viaduct.codegeneration.AbstractCodeGenerator
import io.github.aplcornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.codegeneration.UnsupportedOperatorException
import io.github.aplcornell.viaduct.codegeneration.typeTranslator
import io.github.aplcornell.viaduct.selection.ProtocolCommunication
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Operator
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.datatypes.Get
import io.github.aplcornell.viaduct.syntax.datatypes.Modify
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.LiteralNode
import io.github.aplcornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.operators.Addition
import io.github.aplcornell.viaduct.syntax.operators.And
import io.github.aplcornell.viaduct.syntax.operators.Division
import io.github.aplcornell.viaduct.syntax.operators.EqualTo
import io.github.aplcornell.viaduct.syntax.operators.ExclusiveOr
import io.github.aplcornell.viaduct.syntax.operators.GreaterThan
import io.github.aplcornell.viaduct.syntax.operators.GreaterThanOrEqualTo
import io.github.aplcornell.viaduct.syntax.operators.LessThan
import io.github.aplcornell.viaduct.syntax.operators.LessThanOrEqualTo
import io.github.aplcornell.viaduct.syntax.operators.Maximum
import io.github.aplcornell.viaduct.syntax.operators.Minimum
import io.github.aplcornell.viaduct.syntax.operators.Multiplication
import io.github.aplcornell.viaduct.syntax.operators.Mux
import io.github.aplcornell.viaduct.syntax.operators.Negation
import io.github.aplcornell.viaduct.syntax.operators.Not
import io.github.aplcornell.viaduct.syntax.operators.Or
import io.github.aplcornell.viaduct.syntax.operators.Subtraction
import io.github.aplcornell.viaduct.syntax.types.BooleanType
import io.github.aplcornell.viaduct.syntax.types.ImmutableCellType
import io.github.aplcornell.viaduct.syntax.types.IntegerType
import io.github.aplcornell.viaduct.syntax.types.MutableCellType
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.syntax.types.VectorType
import io.github.aplcornell.viaduct.syntax.values.BooleanValue
import io.github.aplcornell.viaduct.syntax.values.IntegerValue
import io.github.aplcornell.viaduct.syntax.values.Value
import java.math.BigInteger

private data class ABYPair(val server: Host, val client: Host)

class ABYCodeGenerator(
    context: CodeGeneratorContext,
) : AbstractCodeGenerator(context) {
    private val typeAnalysis: TypeAnalysis = TypeAnalysis.get(context.program)
    private val nameAnalysis: NameAnalysis = NameAnalysis.get(context.program)
    private val protocolAnalysis: ProtocolAnalysis = ProtocolAnalysis(context.program, context.protocolComposer)
    private var protocolToABYPartyMap: MutableMap<ABYPair, String> = mutableMapOf()

    companion object {
        const val BIT_LENGTH: Int = 32
    }

    private fun role(protocol: Protocol, host: Host): Role =
        if ((protocol as ABY).client == host) {
            Role.CLIENT
        } else {
            Role.SERVER
        }

    private fun address(protocol: ABY, host: Host) =
        if (role(protocol, host) == Role.SERVER) {
            CodeBlock.of("%S", "")
        } else {
            CodeBlock.of("%L.hostName", context.url(protocol.server))
        }

    private fun abyParty(protocol: ABY, role: Role, port: String): CodeBlock =
        CodeBlock.of(
            "ABYParty(%L, %L, %L, %N.%M(), %L)",
            roleToCodeBlock(role),
            address(protocol, context.host),
            port,
            "Aby",
            MemberName(Aby::class.asClassName(), "getLT"), // TODO make this not hard coded
            BIT_LENGTH,
        )

    private fun abyPartySetup(protocol: Protocol, role: Role): CodeBlock {
        val abyPartyBuilder = CodeBlock.builder()
        val portVarName = context.newTemporary("port")
        when (role) {
            Role.SERVER -> {
                abyPartyBuilder.beginControlFlow("run")
                abyPartyBuilder.addStatement(
                    "val %N = %M()",
                    portVarName,
                    MemberName(
                        "io.github.aplcornell.viaduct.runtime",
                        "findAvailableTcpPort",
                    ),
                )

                abyPartyBuilder.addStatement(
                    "%L",
                    context.send(CodeBlock.of(portVarName), (protocol as ABY).client),
                )

                abyPartyBuilder.addStatement("%L", abyParty(protocol, role, portVarName))
                abyPartyBuilder.endControlFlow()
            }

            Role.CLIENT -> {
                abyPartyBuilder.beginControlFlow("run")
                abyPartyBuilder.addStatement(
                    "val %N = %L",
                    portVarName,
                    context.receive(INT, (protocol as ABY).server),
                )

                abyPartyBuilder.addStatement("%L", abyParty(protocol, role, portVarName))
                abyPartyBuilder.endControlFlow()
            }

            else -> throw IllegalArgumentException("Unknown ABY Role: $role.")
        }
        return abyPartyBuilder.build()
    }

    override fun setup(protocol: Protocol): List<PropertySpec> {
        return if (protocolToABYPartyMap.containsKey(ABYPair((protocol as ABY).server, protocol.client))) {
            listOf()
        } else {
            listOf(
                PropertySpec.builder(
                    protocolToABYPartyMap.getOrPut(
                        ABYPair(protocol.server, protocol.client),
                    ) { context.newTemporary("abyParty") },
                    ABYParty::class,
                ).initializer(
                    abyPartySetup(protocol, role(protocol, context.host)),
                ).addModifiers(KModifier.PRIVATE).build(),
            )
        }
    }

    private fun addConversionGates(
        destProtocol: Protocol,
        sourceProtocol: Protocol,
        kotlinName: String,
    ): CodeBlock {
        if (sourceProtocol !is ABY || destProtocol !is ABY) {
            return CodeBlock.of("")
        }

        return when (sourceProtocol) {
            is YaoABY -> {
                when (destProtocol) {
                    is YaoABY -> CodeBlock.of("")
                    is BoolABY -> CodeBlock.of(".putY2BGate(%L)", kotlinName)
                    is ArithABY ->
                        CodeBlock.of(
                            ".putB2AGate(%L.putY2BGate(%L))",
                            protocolToAbyPartyCircuit(sourceProtocol, SharingType.S_BOOL),
                            kotlinName,
                        )
                }
            }

            is BoolABY -> {
                when (destProtocol) {
                    is YaoABY -> CodeBlock.of(".putB2YGate(%L)", kotlinName)
                    is BoolABY -> CodeBlock.of("")
                    is ArithABY -> CodeBlock.of(".putB2AGate(%L)", kotlinName)
                }
            }

            is ArithABY -> {
                when (destProtocol) {
                    is YaoABY -> CodeBlock.of(".putA2YGate(%L)", kotlinName)
                    is BoolABY ->
                        CodeBlock.of(
                            ".putY2BGate(%L.putA2YGate(%L))",
                            protocolToAbyPartyCircuit(sourceProtocol, SharingType.S_YAO),
                            kotlinName,
                        )

                    is ArithABY -> CodeBlock.of("")
                }
            }
        }
    }

    private fun protocolToShareType(protocol: ABY): SharingType =
        when (protocol) {
            is ArithABY -> SharingType.S_ARITH
            is BoolABY -> SharingType.S_BOOL
            is YaoABY -> SharingType.S_YAO
        }

    private fun protocolToAbyPartyCircuit(
        protocol: Protocol,
        shareType: SharingType = protocolToShareType(protocol as ABY),
    ): CodeBlock {
        if (protocol !is ABY) {
            return CodeBlock.of("%L", "")
        }
        return CodeBlock.of(
            "%L.getCircuitBuilder(%T.%L)",
            protocolToABYPartyMap.getValue(ABYPair(protocol.server, protocol.client)),
            shareType::class.asClassName(),
            shareType,
        )
    }

    private fun valueToShare(value: Value, protocol: Protocol): CodeBlock =
        when (value) {
            is BooleanValue ->
                CodeBlock.of(
                    "%L.putCONSGate(%L.compareTo(false).toBigInteger(), %L)",
                    protocolToAbyPartyCircuit(protocol),
                    value.value,
                    BIT_LENGTH,
                )

            is IntegerValue ->
                CodeBlock.of(
                    "%L.putCONSGate(%L.toBigInteger(), %L)",
                    protocolToAbyPartyCircuit(protocol),
                    value.value,
                    BIT_LENGTH,
                )

            else -> throw java.lang.IllegalArgumentException("Unknown value type: $value.")
        }

    private fun binaryOpToShare(
        circuit: CodeBlock,
        gateMethod: CodeBlock,
        arg1: CodeBlock,
        arg2: CodeBlock,
    ): CodeBlock =
        CodeBlock.of("%L.%L(%L, %L)", circuit, gateMethod, arg1, arg2)

    private fun shareOfOperatorApplication(protocol: Protocol, op: Operator, args: List<CodeBlock>): CodeBlock =
        when (op) {
            Minimum ->
                CodeBlock.of(
                    "%M(%L, %L, %L)",
                    MemberName("io.github.apl_cornell.aby.Aby", "putMinGate"),
                    protocolToAbyPartyCircuit(protocol),
                    args.first(),
                    args.last(),
                )

            Maximum ->
                CodeBlock.of(
                    "%M(%L, %L, %L)",
                    MemberName("io.github.apl_cornell.aby.Aby", "putMaxGate"),
                    protocolToAbyPartyCircuit(protocol),
                    args.first(),
                    args.last(),
                )

            Negation ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putSUBGate"),
                    CodeBlock.of(
                        "%L.putCONSGate(%T.ZERO, %L)",
                        protocolToAbyPartyCircuit(protocol),
                        BigInteger::class.asClassName(),
                        BIT_LENGTH,
                    ),
                    args.first(),
                )

            Addition ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putADDGate"),
                    args.first(),
                    args.last(),
                )

            Subtraction ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putSUBGate"),
                    args.first(),
                    args.last(),
                )

            Multiplication ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putMULGate"),
                    args.first(),
                    args.last(),
                )

            Not ->
                CodeBlock.of(
                    "%L.%M(%L)",
                    protocolToAbyPartyCircuit(protocol),
                    MemberName("io.github.aplcornell.viaduct.runtime.aby", "putNOTGate"),
                    args.first(),
                )

            And ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putANDGate"),
                    args.first(),
                    args.last(),
                )

            // a | b = ~(~a & ~b)
            Or ->
                CodeBlock.of(
                    "%L.%L(%L)",
                    protocolToAbyPartyCircuit(protocol),
                    "putNOTGate",
                    binaryOpToShare(
                        protocolToAbyPartyCircuit(protocol),
                        CodeBlock.of("putANDGate"),
                        CodeBlock.of(
                            "%L.%M(%L)",
                            protocolToAbyPartyCircuit(protocol),
                            MemberName("io.github.aplcornell.viaduct.runtime.aby", "putNOTGate"),
                            args.first(),
                        ),
                        CodeBlock.of(
                            "%L.%M(%L)",
                            protocolToAbyPartyCircuit(protocol),
                            MemberName("io.github.aplcornell.viaduct.runtime.aby", "putNOTGate"),
                            args.last(),
                        ),
                    ),
                )

            EqualTo ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putEQGate"),
                    args.first(),
                    args.last(),
                )

            LessThan ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putGTGate"),
                    args.last(),
                    args.first(),
                )

            // (x <= y) <=> not (x > y)
            LessThanOrEqualTo ->
                CodeBlock.of(
                    "%L.%L(%L)",
                    protocolToAbyPartyCircuit(protocol),
                    "putNOTGate",
                    binaryOpToShare(
                        protocolToAbyPartyCircuit(protocol),
                        CodeBlock.of("putGTGate"),
                        args.first(),
                        args.last(),
                    ),
                )

            GreaterThan ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putGTGate"),
                    args.first(),
                    args.last(),
                )

            // (x >= y) <=> not (x < y)
            GreaterThanOrEqualTo ->
                CodeBlock.of(
                    "%L.%L(%L)",
                    protocolToAbyPartyCircuit(protocol),
                    "putNOTGate",
                    binaryOpToShare(
                        protocolToAbyPartyCircuit(protocol),
                        CodeBlock.of("putLTGate"),
                        args.first(),
                        args.last(),
                    ),
                )

            Mux ->
                CodeBlock.of(
                    "%L.%L(%L, %L, %L)",
                    protocolToAbyPartyCircuit(protocol),
                    "putMUXGate",
                    args.first(),
                    args[1],
                    args.last(),
                )

            ExclusiveOr ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putXORGate"),
                    args.first(),
                    args.last(),
                )

            Division ->
                CodeBlock.of(
                    "%M(%L, %L, %L)",
                    MemberName("io.github.apl_cornell.aby.Aby", "putInt32DIVGate"),
                    protocolToAbyPartyCircuit(protocol),
                    args.last(),
                    args.first(),

                )

            else -> throw UnsupportedOperationException("Unknown operator $op.")
        }

    override fun kotlinType(protocol: Protocol, sourceType: ValueType): TypeName = (Share::class).asTypeName()

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> valueToShare(expr.value, protocol)

            is ReadNode -> {
                val conversion = addConversionGates(
                    protocol, // destination protocol
                    protocolAnalysis.primaryProtocol(expr), // source protocol
                    context.kotlinName(expr.temporary.value, protocolAnalysis.primaryProtocol(expr)), // share name
                )
                if (conversion != CodeBlock.of("")) {
                    CodeBlock.of(
                        "%L%L",
                        protocolToAbyPartyCircuit(protocol),
                        conversion,
                    )
                } else {
                    CodeBlock.of("%N", context.kotlinName(expr.temporary.value, protocol))
                }
            }

            is OperatorApplicationNode ->
                shareOfOperatorApplication(
                    protocol,
                    expr.operator,
                    expr.arguments.map { exp(protocol, it) },
                )

            // only generate code for the secret query case, otherwise call super
            is QueryNode ->
                when (typeAnalysis.type(nameAnalysis.declaration(expr))) {
                    is VectorType ->
                        when (expr.query.value) {
                            is Get ->
                                when (clearArgument(expr.arguments.first())) {
                                    false ->
                                        CodeBlock.of(
                                            "%L(%L, %L)",
                                            "secretIndexQuery",
                                            exp(protocol, expr.arguments.first()),
                                            context.kotlinName(expr.variable.value),

                                        )

                                    true ->
                                        CodeBlock.of(
                                            "%N[%L]",
                                            context.kotlinName(expr.variable.value),
                                            cleartextExp(protocol, expr.arguments.first()),
                                        )
                                }

                            else -> super.exp(protocol, expr)
                        }

                    is ImmutableCellType ->
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(context.kotlinName(expr.variable.value))
                            else -> super.exp(protocol, expr)
                        }

                    is MutableCellType ->
                        when (expr.query.value) {
                            is Get -> CodeBlock.of("%N.get()", context.kotlinName(expr.variable.value))
                            else -> super.exp(protocol, expr)
                        }

                    else -> super.exp(protocol, expr)
                }

            else -> super.exp(protocol, expr)
        }

    private fun clearArgument(arg: AtomicExpressionNode): Boolean =
        when (arg) {
            is LiteralNode -> true
            is ReadNode -> protocolAnalysis.relevantCommunicationEvents(arg)
                .all { event -> event.recv.id == ABY.CLEARTEXT_INPUT }
        }

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is VectorType -> {
                when (clearArgument(stmt.arguments.first())) {
                    false -> when (stmt.update.value) {
                        is io.github.aplcornell.viaduct.syntax.datatypes.Set -> {
                            CodeBlock.of(
                                "%N.%N(%L, %L, %L)",
                                context.kotlinName(stmt.variable.value),
                                "secretUpdateSet",
                                protocolToAbyPartyCircuit(protocol),
                                exp(protocol, stmt.arguments.first()),
                                exp(protocol, stmt.arguments.last()),
                            )
                        }

                        is Modify -> {
                            CodeBlock.of(
                                "%N.%N(%N, %N, %L)",
                                context.kotlinName(stmt.variable.value),
                                "secretUpdateModify",
                                protocolToAbyPartyCircuit(protocol),
                                exp(protocol, stmt.arguments.first()),
                                shareOfOperatorApplication(
                                    protocol,
                                    stmt.update.value.operator,
                                    listOf(
                                        CodeBlock.of("it"),
                                        exp(protocol, stmt.arguments.last()),
                                    ),
                                ),
                            )
                        }

                        else -> throw UnsupportedOperatorException(protocol, stmt)
                    }

                    true -> when (stmt.update.value) {
                        is io.github.aplcornell.viaduct.syntax.datatypes.Set -> {
                            CodeBlock.of(
                                "%N[%L] = %L",
                                context.kotlinName(stmt.variable.value),
                                cleartextExp(protocol, stmt.arguments.first()),
                                exp(protocol, stmt.arguments.last()),
                            )
                        }

                        is Modify -> {
                            CodeBlock.of(
                                "%N[%L] = %L",
                                context.kotlinName(stmt.variable.value),
                                cleartextExp(protocol, stmt.arguments.first()),
                                shareOfOperatorApplication(
                                    protocol,
                                    stmt.update.value.operator,
                                    listOf(
                                        CodeBlock.of(
                                            "%N[%L]",
                                            context.kotlinName(stmt.variable.value),
                                            cleartextExp(protocol, stmt.arguments.first()),
                                        ),
                                        exp(protocol, stmt.arguments.last()),
                                    ),
                                ),
                            )
                        }

                        else -> throw UnsupportedOperatorException(protocol, stmt)
                    }
                }
            }

            is MutableCellType ->
                when (stmt.update.value) {
                    is io.github.aplcornell.viaduct.syntax.datatypes.Set -> {
                        CodeBlock.of(
                            "%N.set(%L)",
                            context.kotlinName(stmt.variable.value),
                            exp(protocol, stmt.arguments.first()),
                        )
                    }

                    is Modify -> {
                        CodeBlock.of(
                            "%N = %L",
                            context.kotlinName(stmt.variable.value),
                            shareOfOperatorApplication(
                                protocol,
                                stmt.update.value.operator,
                                listOf(
                                    CodeBlock.of(context.kotlinName(stmt.variable.value)),
                                    exp(protocol, stmt.arguments.first()),
                                ),
                            ),
                        )
                    }

                    else -> throw UnsupportedOperatorException(protocol, stmt)
                }

            else -> throw UnsupportedOperatorException(protocol, stmt)
        }

    private fun roleToCodeBlock(role: Role): CodeBlock = CodeBlock.of("%T.%L", role::class.asClassName(), role)

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication,
    ): CodeBlock {
        if (receiveProtocol is ABY) {
            return CodeBlock.of("")
        }
        val outBuilder = CodeBlock.builder()
        val outShareName = context.newTemporary("outShare")

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
                    throw IllegalArgumentException("ABY: at least one party must receive output when executing circuit.")
            }

        outBuilder.addStatement(
            "val %L = %L.putOUTGate(%L, %L)",
            outShareName,
            protocolToAbyPartyCircuit(sendProtocol),
            context.kotlinName(sender.name.value, sendProtocol),
            outRole,
        )

        // execute circuit
        outBuilder.addStatement(
            "%L.execCircuit()",
            protocolToABYPartyMap[ABYPair(sendProtocol.server, sendProtocol.client)],
        )

        for (event in events.filter { event -> event.send.host == context.host }) {
            when (typeAnalysis.type(sender)) {
                is BooleanType ->
                    outBuilder.addStatement(
                        "%L",
                        context.send(
                            CodeBlock.of(
                                "%L.getClearValue32().%M",
                                outShareName,
                                MemberName("io.github.aplcornell.viaduct.runtime.aby", "bool"),
                            ),
                            event.recv.host,
                        ),
                    )

                is IntegerType ->
                    outBuilder.addStatement(
                        "%L",
                        context.send(
                            CodeBlock.of("%L.getClearValue32().toInt()", outShareName),
                            event.recv.host,
                        ),
                    )
            }
        }

        // reset circuit
        outBuilder.addStatement(
            "%L.reset()",
            protocolToABYPartyMap[ABYPair(sendProtocol.server, sendProtocol.client)],
        )

        return outBuilder.build()
    }

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication,
    ): CodeBlock {
        val receiveBuilder = CodeBlock.builder()
        for (event in events) {
            when {
                // secret input for this host; create input gate
                event.recv.id == ABY.SECRET_INPUT && event.recv.host == context.host -> {
                    when (typeAnalysis.type(sender)) {
                        is BooleanType ->
                            receiveBuilder.addStatement(
                                "val %L = %L.putINGate(%L.compareTo(false).toBigInteger(), %L, %L)",
                                context.kotlinName(sender.name.value, receiveProtocol),
                                protocolToAbyPartyCircuit(receiveProtocol),
                                context.receive(typeTranslator(typeAnalysis.type(sender)), event.send.host),
                                BIT_LENGTH,
                                roleToCodeBlock(role(receiveProtocol, context.host)),
                            )

                        is IntegerType ->
                            receiveBuilder.addStatement(
                                "val %L = %L.putINGate(%L.toBigInteger(), %L, %L)",
                                context.kotlinName(sender.name.value, receiveProtocol),
                                protocolToAbyPartyCircuit(receiveProtocol),
                                context.receive(typeTranslator(typeAnalysis.type(sender)), event.send.host),
                                BIT_LENGTH,
                                roleToCodeBlock(role(receiveProtocol, context.host)),
                            )
                    }
                }

                // other host has secret input; create dummy gate
                event.recv.id == ABY.SECRET_INPUT && event.recv.host != context.host -> {
                    receiveBuilder.addStatement(
                        "val %L = %L.putDummyINGate(%L)",
                        context.kotlinName(sender.name.value, receiveProtocol),
                        protocolToAbyPartyCircuit(receiveProtocol),
                        BIT_LENGTH,
                    )
                }

                // TODO() - look into how to take from the send queue in this case
                // cleartext input; create constant gate
                event.recv.id == ABY.CLEARTEXT_INPUT && event.recv.host == context.host -> {
                    receiveBuilder.add(exp(sendProtocol, sender.value))
                }
            }
        }

        return receiveBuilder.build()
    }
}
