package edu.cornell.cs.apl.viaduct.backends.aby

import com.github.apl_cornell.aby.ABYParty
import com.github.apl_cornell.aby.Aby
import com.github.apl_cornell.aby.Role
import com.github.apl_cornell.aby.SharingType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.withIndent
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.codegeneration.AbstractCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.UnsupportedOperatorException
import edu.cornell.cs.apl.viaduct.codegeneration.typeTranslator
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.BinaryOperator
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.UnaryOperator
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
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
import edu.cornell.cs.apl.viaduct.syntax.operators.And
import edu.cornell.cs.apl.viaduct.syntax.operators.Division
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.ExclusiveOr
import edu.cornell.cs.apl.viaduct.syntax.operators.GreaterThan
import edu.cornell.cs.apl.viaduct.syntax.operators.GreaterThanOrEqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThan
import edu.cornell.cs.apl.viaduct.syntax.operators.LessThanOrEqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import edu.cornell.cs.apl.viaduct.syntax.operators.Mux
import edu.cornell.cs.apl.viaduct.syntax.operators.Negation
import edu.cornell.cs.apl.viaduct.syntax.operators.Not
import edu.cornell.cs.apl.viaduct.syntax.operators.Or
import edu.cornell.cs.apl.viaduct.syntax.operators.Subtraction
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.math.BigInteger

private data class ABYPair(val server: Host, val client: Host)

class ABYCodeGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    private val typeAnalysis: TypeAnalysis = TypeAnalysis.get(context.program)
    private val nameAnalysis: NameAnalysis = NameAnalysis.get(context.program)
    private val protocolAnalysis: ProtocolAnalysis = ProtocolAnalysis(context.program, context.protocolComposer)
    private var protocolToABYPartyMap: MutableMap<ABYPair, String> = mutableMapOf()

    private fun role(protocol: Protocol, host: Host): Role =
        when (protocol) {
            is ABY -> if (protocol.client == host) {
                Role.CLIENT
            } else {
                Role.SERVER
            }
            else -> throw UnsupportedOperationException("unknown protocol: ${protocol.toDocument().print()}")
        }

    private fun abyParty(protocol: ABY, role: Role, port: String): CodeBlock =
        CodeBlock.of(
            "ABYParty(%L, %L.hostName, %L, %N.%M(), %L)",
            roleToCodeBlock(role),
            context.url(protocol.server),
            port,
            "Aby",
            MemberName(Aby::class.asClassName(), "getLT"), // TODO make this not hard coded
            32 // TODO() - where is best place to store this value?
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
                        "edu.cornell.cs.apl.viaduct.runtime",
                        "findAvailableTcpPort"
                    )
                )

                abyPartyBuilder.addStatement(
                    "%L",
                    context.send(CodeBlock.of(portVarName), (protocol as ABY).client)
                )

                abyPartyBuilder.addStatement("%L", abyParty(protocol, role, portVarName))
                abyPartyBuilder.endControlFlow()
            }

            Role.CLIENT -> {
                abyPartyBuilder.beginControlFlow("run")
                abyPartyBuilder.addStatement(
                    "val %N = %L",
                    portVarName,
                    context.receive(INT, (protocol as ABY).server)
                )

                abyPartyBuilder.addStatement("%L", abyParty(protocol, role, portVarName))
                abyPartyBuilder.endControlFlow()
            }
            else -> throw UnsupportedOperationException("Unknown ABY Role: $role")
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
                        ABYPair(protocol.server, protocol.client)
                    ) { context.newTemporary("abyParty") },
                    ABYParty::class
                ).initializer(
                    abyPartySetup(protocol, role(protocol, context.host))
                ).addModifiers(KModifier.PRIVATE).build()
            )
        }
    }

    private val bitLen = 32

    private fun addConversionGates(
        destProtocol: Protocol,
        sourceProtocol: Protocol,
        kotlinName: String,
        circuitBuilder: CodeBlock
    ): CodeBlock =
        when (sourceProtocol) {
            is YaoABY -> {
                when (destProtocol) {
                    is YaoABY -> CodeBlock.of("")
                    is BoolABY -> CodeBlock.of(".putY2BGate(%L)", kotlinName)
                    is ArithABY -> CodeBlock.of(".putB2AGate(%L.putY2BGate(%L))", circuitBuilder, kotlinName)
                    else -> throw UnsupportedOperationException(
                        "unsupported ABY protocol: ${sourceProtocol.toDocument().print()}"
                    )
                }
            }
            is BoolABY -> {
                when (destProtocol) {
                    is YaoABY -> CodeBlock.of(".putB2YGate(%L)", kotlinName)
                    is BoolABY -> CodeBlock.of("")
                    is ArithABY -> CodeBlock.of(".putB2AGate(%L)", kotlinName)
                    else -> throw UnsupportedOperationException(
                        "unsupported ABY protocol: ${sourceProtocol.toDocument().print()}"
                    )
                }
            }
            is ArithABY -> {
                when (destProtocol) {
                    is YaoABY -> CodeBlock.of(".putA2YGate(%L)", kotlinName)
                    is BoolABY -> CodeBlock.of(".putY2BGate(%L.putA2YGate(%L))", circuitBuilder, kotlinName)
                    is ArithABY -> CodeBlock.of("")
                    else -> throw UnsupportedOperationException(
                        "unsupported ABY protocol: ${sourceProtocol.toDocument().print()}"
                    )
                }
            }
            else -> CodeBlock.of("")
        }

    private fun protocolToShareType(protocol: Protocol): CodeBlock =
        when (protocol) {
            is ArithABY -> CodeBlock.of("%T.S_ARITH", SharingType::class.asClassName())
            is BoolABY -> CodeBlock.of("%T.S_BOOL", SharingType::class.asClassName())
            is YaoABY -> CodeBlock.of("%T.S_YAO", SharingType::class.asClassName())
            else -> throw UnsupportedOperationException(
                "unsupported protocol: ${protocol.toDocument().print()}"
            )
        }

    private fun protocolToAbyPartyCircuit(protocol: Protocol): CodeBlock =
        CodeBlock.of(
            "%L.getCircuitBuilder(%L)",
            protocolToABYPartyMap.getValue(ABYPair((protocol as ABY).server, protocol.client)),
            protocolToShareType(protocol)
        )

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
                    "%L.putCONSGate(%L.toBigInteger(), %L)",
                    protocolToAbyPartyCircuit(protocol),
                    value.value,
                    bitLen
                )
            else -> throw UnsupportedOperationException("unknown value type: ${value.toDocument().print()}")
        }

    private fun binaryOpToShare(
        circuit: CodeBlock,
        gateMethod: CodeBlock,
        arg1: CodeBlock,
        arg2: CodeBlock
    ): CodeBlock =
        CodeBlock.of("%L.%L(%L, %L)", circuit, gateMethod, arg1, arg2)

    private fun shareOfOperatorApplication(protocol: Protocol, op: Operator, args: List<CodeBlock>): CodeBlock =
        when (op) {
            Minimum ->
                CodeBlock.of(
                    "%M(%L, %L, %L)",
                    MemberName("com.github.apl_cornell.aby.Aby", "putMinGate"),
                    protocolToAbyPartyCircuit(protocol),
                    args.first(),
                    args.last()
                )

            Maximum ->
                CodeBlock.of(
                    "%M(%L, %L, %L)",
                    MemberName("com.github.apl_cornell.aby.Aby", "putMaxGate"),
                    protocolToAbyPartyCircuit(protocol),
                    args.first(),
                    args.last()
                )

            Negation ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putSUBGate"),
                    CodeBlock.of(
                        "%L.putCONSGate(%T.ZERO, %L)",
                        protocolToAbyPartyCircuit(protocol),
                        BigInteger::class.asClassName(),
                        bitLen
                    ),
                    args.first()
                )

            Addition ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putADDGate"),
                    args.first(),
                    args.last()
                )

            Subtraction ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putSUBGate"),
                    args.first(),
                    args.last()
                )

            Multiplication ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putMULGate"),
                    args.first(),
                    args.last()
                )

            Not ->
                CodeBlock.of(
                    "%L.%M(%L)",
                    protocolToAbyPartyCircuit(protocol),
                    MemberName("edu.cornell.cs.apl.viaduct.runtime.aby", "putNOTGate"),
                    args.first()
                )

            And ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putANDGate"),
                    args.first(),
                    args.last()
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
                            MemberName("edu.cornell.cs.apl.viaduct.runtime.aby", "putNOTGate"),
                            args.first()
                        ),
                        CodeBlock.of(
                            "%L.%M(%L)",
                            protocolToAbyPartyCircuit(protocol),
                            MemberName("edu.cornell.cs.apl.viaduct.runtime.aby", "putNOTGate"),
                            args.last()
                        )
                    )
                )

            EqualTo ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putEQGate"),
                    args.first(),
                    args.last()
                )

            LessThan ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putGTGate"),
                    args.last(),
                    args.first()
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
                        args.last()
                    )
                )

            GreaterThan ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putGTGate"),
                    args.first(),
                    args.last()
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
                        args.last()
                    )
                )

            Mux ->
                CodeBlock.of(
                    "%L.%L(%L, %L, %L)",
                    protocolToAbyPartyCircuit(protocol),
                    "putMUXGate",
                    args.first(),
                    args[1],
                    args.last()
                )

            ExclusiveOr ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putXORGate"),
                    args.first(),
                    args.last()
                )

            Division ->
                CodeBlock.of(
                    "%M(%L, %L, %L)",
                    MemberName("com.github.apl_cornell.aby.Aby", "putInt32DIVGate"),
                    protocolToAbyPartyCircuit(protocol),
                    args.first(),
                    args.last()
                )
            else -> throw UnsupportedOperationException("unknown operator")
        }

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> valueToShare(expr.value, protocol)

            is ReadNode -> {
                val conversion = addConversionGates(
                    protocol, // destination protocol
                    protocolAnalysis.primaryProtocol(expr), // source protocol
                    context.kotlinName(expr.temporary.value, protocolAnalysis.primaryProtocol(expr)), // share name
                    protocolToAbyPartyCircuit(protocol) // circuit builder
                )
                if (conversion != CodeBlock.of("")) {
                    CodeBlock.of(
                        "%L%L",
                        protocolToAbyPartyCircuit(protocol),
                        conversion
                    )
                } else {
                    CodeBlock.of("%N", context.kotlinName(expr.temporary.value, protocol))
                }
            }

            is OperatorApplicationNode -> {
                when (expr.operator) {
                    is BinaryOperator ->
                        shareOfOperatorApplication(
                            protocol,
                            expr.operator,
                            listOf(
                                exp(protocol, expr.arguments.first()), exp(protocol, expr.arguments.last())
                            )
                        )

                    is UnaryOperator ->
                        shareOfOperatorApplication(
                            protocol,
                            expr.operator,
                            listOf(exp(protocol, expr.arguments.first()))
                        )

                    // ternary operator
                    is Mux ->
                        shareOfOperatorApplication(
                            protocol,
                            expr.operator,
                            listOf(
                                exp(protocol, expr.arguments.first()),
                                exp(protocol, expr.arguments[1]),
                                exp(protocol, expr.arguments.last())
                            )
                        )

                    else -> throw UnsupportedOperatorException(protocol, expr)
                }
            }

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
                                            context.kotlinName(expr.variable.value)

                                        )
                                    true ->
                                        CodeBlock.of(
                                            "%N[%L]",
                                            context.kotlinName(expr.variable.value),
                                            cleartextExp(protocol, expr.arguments.first())
                                        )
                                }
                            else -> throw UnsupportedOperationException(
                                "unknown query: ${expr.query.toDocument().print()}"
                            )
                        }

                    is ImmutableCellType ->
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(context.kotlinName(expr.variable.value))
                            else -> throw UnsupportedOperationException(
                                "unknown query: ${expr.query.toDocument().print()}"
                            )
                        }

                    is MutableCellType ->
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(context.kotlinName(expr.variable.value))
                            else -> throw UnsupportedOperationException(
                                "unknown query: ${expr.query.toDocument().print()}"
                            )
                        }

                    else -> throw UnsupportedOperationException("unknown AST object: ${expr.toDocument().print()}")
                }

            is DeclassificationNode -> exp(protocol, expr.expression)

            is EndorsementNode -> exp(protocol, expr.expression)

            is DowngradeNode -> exp(protocol, expr.expression)

            is InputNode ->
                throw UnsupportedOperationException("cannot perform I/O in non-Local protocol")
        }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        when (stmt.value) {
            is InputNode -> throw UnsupportedOperationException("cannot perform I/O in non-Local protocol")
            is PureExpressionNode -> {
                CodeBlock.of(
                    "val %N = %L",
                    context.kotlinName(stmt.name.value, protocol),
                    exp(protocol, stmt.value)
                )
            }
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
                        is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                            CodeBlock.of(
                                "%N.%N(%L, %L, %L)",
                                context.kotlinName(stmt.variable.value),
                                "secretUpdateSet",
                                protocolToAbyPartyCircuit(protocol),
                                exp(protocol, stmt.arguments.first()),
                                exp(protocol, stmt.arguments.last())
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
                                        exp(protocol, stmt.arguments.last())
                                    )
                                )
                            )
                        }
                        else -> throw UnsupportedOperationException("unknown update: ${stmt.update.value.toDocument().print()}")
                    }

                    true -> when (stmt.update.value) {
                        is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                            CodeBlock.of(
                                "%N[%L] = %L",
                                context.kotlinName(stmt.variable.value),
                                cleartextExp(protocol, stmt.arguments.first()),
                                exp(protocol, stmt.arguments.last())
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
                                            cleartextExp(protocol, stmt.arguments.first())
                                        ),
                                        exp(protocol, stmt.arguments.last())
                                    )
                                )
                            )
                        }
                        else -> throw UnsupportedOperationException("unknown update: ${stmt.update.value.toDocument().print()}")
                    }
                }
            }

            is MutableCellType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set -> {
                        CodeBlock.of(
                            "%N = %L",
                            context.kotlinName(stmt.variable.value),
                            exp(protocol, stmt.arguments.first())
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
                                    exp(protocol, stmt.arguments.first())
                                )
                            )
                        )
                    }
                    else -> throw UnsupportedOperationException("unknown update: ${stmt.update.value.toDocument().print()}")
                }

            else ->
                throw UnsupportedOperationException("ABY: unknown update for immutable cell: ${stmt.toDocument().print()}")
        }


    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        throw UnsupportedOperationException("cannot perform I/O in non-local protocol: ${stmt.toDocument().print()}")

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        throw UnsupportedOperationException("ABY: Cannot execute conditional guard: ${expr.toDocument().print()}")

    private fun roleToCodeBlock(role: Role): CodeBlock =
        when (role) {
            Role.CLIENT -> CodeBlock.of("%T.CLIENT", Role::class.asClassName())
            Role.SERVER -> CodeBlock.of("%T.SERVER", Role::class.asClassName())
            Role.ALL -> CodeBlock.of("%T.ALL", Role::class.asClassName())
        }

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
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
                    if (thisHostRole == Role.SERVER) roleToCodeBlock(Role.SERVER) else roleToCodeBlock(Role.CLIENT)

                thisHostReceives && otherHostReceives -> roleToCodeBlock(Role.ALL)

                else ->
                    throw ViaductInterpreterError("ABY: at least one party must receive output when executing circuit")
            }

        outBuilder.addStatement(
            "val %L = %L.putOUTGate(%L, %L)",
            outShareName,
            protocolToAbyPartyCircuit(sendProtocol),
            context.kotlinName(sender.name.value, sendProtocol),
            outRole
        )

        // execute circuit
        outBuilder.addStatement(
            "%L.execCircuit()",
            protocolToABYPartyMap[ABYPair(sendProtocol.server, sendProtocol.client)]
        )

        for (event in events) {
            when (typeAnalysis.type(sender)) {
                is BooleanType ->
                    outBuilder.addStatement(
                        "%L",
                        context.send(
                            CodeBlock.of(
                                "%L.getClearValue32().%M",
                                outShareName,
                                MemberName("edu.cornell.cs.apl.viaduct.runtime.aby", "bool")
                            ),
                            event.recv.host
                        )
                    )
                is IntegerType ->
                    outBuilder.addStatement(
                        "%L",
                        context.send(
                            CodeBlock.of("%L.getClearValue32().toInt()", outShareName),
                            event.recv.host
                        )
                    )
            }
        }

        // reset circuit
        outBuilder.addStatement(
            "%L.reset()",
            protocolToABYPartyMap[ABYPair(sendProtocol.server, sendProtocol.client)]
        )

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
                            receiveBuilder.addStatement(
                                "val %L = %L.putINGate(%L.compareTo(false).toBigInteger(), %L, %L)",
                                context.kotlinName(sender.name.value, receiveProtocol),
                                protocolToAbyPartyCircuit(receiveProtocol),
                                context.receive(typeTranslator(typeAnalysis.type(sender)), event.send.host),
                                bitLen,
                                roleToCodeBlock(role(receiveProtocol, context.host))
                            )

                        is IntegerType ->
                            receiveBuilder.addStatement(
                                "val %L = %L.putINGate(%L.toBigInteger(), %L, %L)",
                                context.kotlinName(sender.name.value, receiveProtocol),
                                protocolToAbyPartyCircuit(receiveProtocol),
                                context.receive(typeTranslator(typeAnalysis.type(sender)), event.send.host),
                                bitLen,
                                roleToCodeBlock(role(receiveProtocol, context.host))
                            )
                    }
                }

                // other host has secret input; create dummy gate
                event.recv.id == ABY.SECRET_INPUT && event.recv.host != context.host -> {
                    receiveBuilder.addStatement(
                        "val %L = %L.putDummyINGate(%L)",
                        context.kotlinName(sender.name.value, receiveProtocol),
                        protocolToAbyPartyCircuit(receiveProtocol),
                        bitLen
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
