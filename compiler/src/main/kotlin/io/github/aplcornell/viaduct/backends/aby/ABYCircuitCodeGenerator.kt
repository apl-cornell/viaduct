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
import io.github.apl_cornell.aby.Role
import io.github.apl_cornell.aby.Share
import io.github.apl_cornell.aby.SharingType
import io.github.aplcornell.viaduct.backends.cleartext.Local
import io.github.aplcornell.viaduct.backends.cleartext.Replication
import io.github.aplcornell.viaduct.circuitanalysis.NameAnalysis
import io.github.aplcornell.viaduct.circuitcodegeneration.AbstractCodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.Argument
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.circuitcodegeneration.UnsupportedCommunicationException
import io.github.aplcornell.viaduct.circuitcodegeneration.findAvailableTcpPort
import io.github.aplcornell.viaduct.circuitcodegeneration.lookup
import io.github.aplcornell.viaduct.circuitcodegeneration.new
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.aplcornell.viaduct.syntax.circuit.ExpressionNode
import io.github.aplcornell.viaduct.syntax.circuit.LiteralNode
import io.github.aplcornell.viaduct.syntax.circuit.OperatorNode
import io.github.aplcornell.viaduct.syntax.circuit.ReferenceNode
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
import io.github.aplcornell.viaduct.syntax.types.IntegerType
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.syntax.values.Value
import java.math.BigInteger

class ABYCircuitCodeGenerator(
    context: CodeGeneratorContext,
) : AbstractCodeGenerator(context) {
    private data class ABYPair(val server: Host, val client: Host)

    private val nameAnalysis: NameAnalysis = NameAnalysis.get(context.program)
    private var protocolToABYPartyMap: MutableMap<ABYPair, String> = mutableMapOf()

    // An alias in Kotlin code for the current circuit being built, to make generated code prettier.
    // Updated at the beginning and end of each circuit body.
    private var currentABYPartyCircuit: CodeBlock? = null

    companion object {
        const val BIT_LENGTH: Int = 32
    }

    private fun role(protocol: ABY, host: Host): Role =
        if (protocol.client == host) {
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

    private fun abyParty(protocol: ABY, role: Role, port: CodeBlock): CodeBlock =
        CodeBlock.of(
            "ABYParty(%L, %L, %L)",
            roleToCodeBlock(role),
            address(protocol, context.host),
            port,
        )

    private fun abyPartySetup(protocol: ABY, role: Role): CodeBlock =
        when (role) {
            Role.SERVER -> {
                val builder = CodeBlock.builder()
                val portVar = CodeBlock.of("port")
                builder.beginControlFlow("%L.let { %L ->", findAvailableTcpPort, portVar)
                builder.addStatement(
                    "%L",
                    context.send(portVar, protocol.client),
                )
                builder.addStatement("%L", abyParty(protocol, role, portVar))
                builder.endControlFlow()
                builder.build()
            }

            Role.CLIENT -> {
                abyParty(protocol, role, context.receive(INT, protocol.server))
            }

            else -> throw IllegalArgumentException("Unknown ABY Role: $role.")
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
        destProtocol: ABY,
        sourceProtocol: ABY,
        kotlinName: String,
    ): CodeBlock {
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
        protocol: ABY,
        shareType: SharingType = protocolToShareType(protocol),
    ): CodeBlock {
        return currentABYPartyCircuit ?: CodeBlock.of(
            "%L.getCircuitBuilder(%T.%L)",
            protocolToABYPartyMap.getValue(ABYPair(protocol.server, protocol.client)),
            shareType::class.asClassName(),
            shareType,
        )
    }

    private fun CodeBlock.toInt(type: ValueType): CodeBlock =
        CodeBlock.of(
            "%L.toBigInteger()",
            when (type) {
                is BooleanType -> CodeBlock.of(
                    "%L.compareTo(false)",
                    this,
                )
                is IntegerType -> this
                else -> throw java.lang.IllegalArgumentException("Unknown value type: $type.")
            },
        )

    private fun valueToShare(value: Value, protocol: Protocol): CodeBlock =
        CodeBlock.of("%L", value).toShare(protocol as ABY, value.type)

    private fun CodeBlock.toShare(protocol: ABY, type: ValueType): CodeBlock =
        CodeBlock.of(
            "%L.putCONSGate(%L, %L)",
            protocolToAbyPartyCircuit(protocol),
            this.toInt(type),
            BIT_LENGTH,
        )

    private fun CodeBlock.fromShare(type: ValueType): CodeBlock =
        CodeBlock.of(
            "%L.getClearValue32().%L",
            this,
            when (type) {
                is IntegerType -> "toInt()"
                is BooleanType ->
                    CodeBlock.of("%M", MemberName("io.github.aplcornell.viaduct.runtime.aby", "bool"))
                else -> throw java.lang.IllegalArgumentException("Unknown value type: $type.")
            },
        )

    private fun binaryOpToShare(
        circuit: CodeBlock,
        gateMethod: CodeBlock,
        arg1: CodeBlock,
        arg2: CodeBlock,
    ): CodeBlock =
        CodeBlock.of("%L.%L(%L, %L)", circuit, gateMethod, arg1, arg2)

    override fun operatorApplication(protocol: Protocol, op: OperatorNode, arguments: List<CodeBlock>): CodeBlock {
        require(protocol is ABY)
        return when (op.operator) {
            Minimum ->
                CodeBlock.of(
                    "%M(%L, %L, %L)",
                    MemberName("io.github.apl_cornell.aby.Aby", "putMinGate"),
                    protocolToAbyPartyCircuit(protocol),
                    arguments.first(),
                    arguments.last(),
                )

            Maximum ->
                CodeBlock.of(
                    "%M(%L, %L, %L)",
                    MemberName("io.github.apl_cornell.aby.Aby", "putMaxGate"),
                    protocolToAbyPartyCircuit(protocol),
                    arguments.first(),
                    arguments.last(),
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
                    arguments.first(),
                )

            Addition ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putADDGate"),
                    arguments.first(),
                    arguments.last(),
                )

            Subtraction ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putSUBGate"),
                    arguments.first(),
                    arguments.last(),
                )

            Multiplication ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putMULGate"),
                    arguments.first(),
                    arguments.last(),
                )

            Not ->
                CodeBlock.of(
                    "%L.%M(%L)",
                    protocolToAbyPartyCircuit(protocol),
                    MemberName("io.github.aplcornell.viaduct.runtime.aby", "putNOTGate"),
                    arguments.first(),
                )

            And ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putANDGate"),
                    arguments.first(),
                    arguments.last(),
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
                            arguments.first(),
                        ),
                        CodeBlock.of(
                            "%L.%M(%L)",
                            protocolToAbyPartyCircuit(protocol),
                            MemberName("io.github.aplcornell.viaduct.runtime.aby", "putNOTGate"),
                            arguments.last(),
                        ),
                    ),
                )

            EqualTo ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putEQGate"),
                    arguments.first(),
                    arguments.last(),
                )

            LessThan ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putGTGate"),
                    arguments.last(),
                    arguments.first(),
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
                        arguments.first(),
                        arguments.last(),
                    ),
                )

            GreaterThan ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putGTGate"),
                    arguments.first(),
                    arguments.last(),
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
                        arguments.first(),
                        arguments.last(),
                    ),
                )

            Mux ->
                CodeBlock.of(
                    "%L.%L(%L, %L, %L)",
                    protocolToAbyPartyCircuit(protocol),
                    "putMUXGate",
                    arguments[1],
                    arguments[2],
                    arguments[0],
                )

            ExclusiveOr ->
                binaryOpToShare(
                    protocolToAbyPartyCircuit(protocol),
                    CodeBlock.of("putXORGate"),
                    arguments.first(),
                    arguments.last(),
                )

            Division ->
                CodeBlock.of(
                    "%M(%L, %L, %L)",
                    MemberName("io.github.apl_cornell.aby.Aby", "putInt32DIVGate"),
                    protocolToAbyPartyCircuit(protocol),
                    arguments.last(),
                    arguments.first(),

                )

            else -> throw UnsupportedOperationException("Unknown operator $op.")
        }
    }

    override fun paramType(protocol: Protocol, sourceType: ValueType): TypeName = (Share::class).asTypeName()

    override fun storageType(protocol: Protocol, sourceType: ValueType): TypeName = INT

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> valueToShare(expr.value, protocol)
            is ReferenceNode -> CodeBlock.of("%N", context.kotlinName(expr.name.value))
                .toShare(protocol as ABY, IntegerType)
            else -> super.exp(protocol, expr)
        }

    private fun roleToCodeBlock(role: Role): CodeBlock = CodeBlock.of("%T.%L", role::class.asClassName(), role)

    override fun circuitBody(
        protocol: Protocol,
        circuitDeclaration: CircuitDeclarationNode,
        outParams: List<CodeBlock>,
    ): CodeBlock {
        require(protocol is ABY)
        val builder = CodeBlock.builder()
        val circuit = CodeBlock.of("%N", context.newTemporary("circuit"))
        builder.addStatement("val %L = %L", circuit, protocolToAbyPartyCircuit(protocol))
        currentABYPartyCircuit = circuit
        builder.add(super.circuitBody(protocol, circuitDeclaration, outParams))
        currentABYPartyCircuit = null
        return builder.build()
    }

    override fun import(
        protocol: Protocol,
        arguments: List<Argument>,
    ): Pair<CodeBlock, List<CodeBlock>> {
        require(protocol is ABY)
        require(context.host in protocol.hosts)
        val builder = CodeBlock.builder()
        val values = arguments.map { argument ->
            val inputName = context.newTemporary(argument.value.toString() + "_inShare")
            when {
                argument.protocol is Local && argument.protocol.host in protocol.hosts -> {
                    if (argument.protocol.host == context.host) {
                        builder.addStatement(
                            "val %L = %L",
                            inputName,
                            argument.type.shape.new(context) { indices ->
                                CodeBlock.of(
                                    "%L.putINGate(%L, %L, %L)",
                                    protocolToAbyPartyCircuit(protocol),
                                    argument.value.lookup(indices).toInt(argument.type.elementType.value),
                                    BIT_LENGTH,
                                    roleToCodeBlock(role(protocol, context.host)),
                                )
                            },
                        )
                    } else {
                        builder.addStatement(
                            "val %L = %L",
                            inputName,
                            argument.type.shape.new(context) {
                                CodeBlock.of(
                                    "%L.putDummyINGate(%L)",
                                    protocolToAbyPartyCircuit(protocol),
                                    BIT_LENGTH,
                                )
                            },
                        )
                    }
                }

                argument.protocol is Replication && argument.protocol.hosts == protocol.hosts -> {
                    builder.addStatement(
                        "val %L = %L",
                        inputName,
                        argument.type.shape.new(context) { indices ->
                            CodeBlock.of(
                                "%L.putCONSGate(%L, %L)",
                                protocolToAbyPartyCircuit(protocol),
                                argument.value.lookup(indices).toShare(protocol, argument.type.elementType.value),
                                BIT_LENGTH,
                            )
                        },
                    )
                }

                else -> throw UnsupportedCommunicationException(argument.protocol, protocol, argument.sourceLocation)
            }
            CodeBlock.of("%N", inputName)
        }
        return Pair(builder.build(), values)
    }

    override fun export(
        protocol: Protocol,
        arguments: List<Argument>,
    ): Pair<CodeBlock, List<CodeBlock>> {
        require(protocol is ABY)
        require(context.host in protocol.hosts)
        val builder = CodeBlock.builder()
        val outShareNames = arguments.map { argument ->
            val outShareName = context.newTemporary(argument.value.toString() + "_outShare")
            val outRole = when (argument.protocol) {
                Local(protocol.server) -> Role.SERVER
                Local(protocol.client) -> Role.CLIENT
                Replication(setOf(protocol.server, protocol.client)) -> Role.ALL
                else -> throw IllegalStateException()
            }
            builder.addStatement(
                "val %N = %L",
                outShareName,
                argument.type.shape.new(context) { indices ->
                    CodeBlock.of(
                        "%L.putOUTGate(%L, %L)",
                        protocolToAbyPartyCircuit(protocol),
                        argument.value.lookup(indices),
                        roleToCodeBlock(outRole),
                    )
                },
            )
            CodeBlock.of("%N", outShareName)
        }
        builder.addStatement(
            "%L.execCircuit()",
            protocolToABYPartyMap[ABYPair(protocol.server, protocol.client)],
        )
        val cleartextTmps = arguments.zip(outShareNames).filter { (arg, _) -> (context.host in arg.protocol.hosts) }
            .map { (arg, outShareName) ->
                val cleartextTmp = context.newTemporary("cleartextTmp")
                builder.addStatement(
                    "val %N = %L",
                    cleartextTmp,
                    arg.type.shape.new(context) { indices ->
                        outShareName.lookup(indices).fromShare(arg.type.elementType.value)
                    },
                )
                CodeBlock.of(cleartextTmp)
            }
        builder.addStatement(
            "%L.reset()",
            protocolToABYPartyMap[ABYPair(protocol.server, protocol.client)],
        )
        return Pair(builder.build(), cleartextTmps)
    }
}
