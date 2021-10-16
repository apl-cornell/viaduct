package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.RuntimeError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.runtime.commitment.Committed
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.ByteVecType
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.StringType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType

class CommitmentCreatorGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    private val typeAnalysis = TypeAnalysis.get(context.program)
    private val nameAnalysis = NameAnalysis.get(context.program)
    private val protocolAnalysis = ProtocolAnalysis(context.program, SimpleProtocolComposer)
    private val runtimeErrorClass = RuntimeError::class

    private val committedClassName = Committed::class

    override fun exp(expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(
                "%T(%L)",
                committedClassName,
                expr.value
            )

            is ReadNode ->
                CodeBlock.of(
                    "%N",
                    context.kotlinName(
                        expr.temporary.value,
                        protocolAnalysis.primaryProtocol(expr)
                    )
                )

            is DowngradeNode -> exp(expr.expression)

            is QueryNode -> {
                when (this.typeAnalysis.type(nameAnalysis.declaration(expr))) {
                    is VectorType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(
                                "%N[%L]",
                                context.kotlinName(expr.variable.value),
                                exp(expr.arguments.first())
                            )
                            else -> throw CodeGenerationError("unknown vector query", expr)
                        }
                    }

                    is ImmutableCellType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(context.kotlinName(expr.variable.value))
                            else -> throw CodeGenerationError("unknown query", expr)
                        }
                    }

                    is MutableCellType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(context.kotlinName(expr.variable.value))
                            else -> throw CodeGenerationError("unknown query", expr)
                        }
                    }

                    else -> throw CodeGenerationError("unknown AST object", expr)
                }
            }

            is OperatorApplicationNode ->
                throw CodeGenerationError("Commitment: cannot perform operations on committed values")

            is InputNode ->
                throw CodeGenerationError("Commitment: cannot perform I/O in non-local protocol")

            is ReceiveNode ->
                throw IllegalInternalCommunicationError(expr)
        }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        CodeBlock.of(
            "val %N = %L",
            context.kotlinName(stmt.temporary.value, protocol),
            exp(stmt.value)
        )

    override fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock {
        TODO("Not yet implemented")
    }

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock {
        TODO("Not yet implemented")
    }

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock {
        throw ViaductInterpreterError("Commitment: cannot perform I/O in non-local protocol")
    }

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock = exp(expr)

    private fun typeTranslator(viaductType: ValueType): TypeName =
        when (viaductType) {
            ByteVecType -> U_BYTE_ARRAY
            BooleanType -> BOOLEAN
            IntegerType -> INT
            StringType -> STRING
            else -> throw CodeGenerationError("unknown send and receive type")
        }

    override fun send(
        sendingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()
        if (sendProtocol != receiveProtocol) {
            val relevantEvents: Set<CommunicationEvent> =
                events.getProjectionSends(ProtocolProjection(sendProtocol, sendingHost))

            for (event in relevantEvents) {

                // send temporary containing hash value
                sendBuilder.addStatement(
                    "%L",
                    context.send(
                        CodeBlock.of("%L", context.kotlinName(sender.temporary.value, sendProtocol)),
                        event.recv.host
                    )
                )
            }
        }
        return sendBuilder.build()
    }

    override fun receive(
        receivingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val receiveBuilder = CodeBlock.builder()
        val projection = ProtocolProjection(receiveProtocol, receivingHost)
        val hashHosts: Set<Host> = (projection.protocol as Commitment).hashHosts
        val clearTextTemp = context.newTemporary("clearTextTemp")
        val commitTemp = context.newTemporary("commitment")
        if (sendProtocol != receiveProtocol) {
            when {
                events.any { event -> event.recv.id == Commitment.CLEARTEXT_INPUT } -> {
                    var relevantEvents =
                        events.getHostReceives(projection.host, Commitment.CLEARTEXT_INPUT)

                    if (relevantEvents.isNotEmpty()) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            clearTextTemp,
                            context.receive(
                                typeTranslator(typeAnalysis.type(sender)),
                                relevantEvents.first().send.host
                            )
                        )
                        relevantEvents = relevantEvents.minusElement(relevantEvents.first())
                    }

                    // receive from the rest of the hosts and compare against clearTextValue
                    for (event in relevantEvents) {

                        // check to make sure that you got the same data from all hosts
                        receiveBuilder.beginControlFlow(
                            "if (%N != %L)",
                            clearTextTemp,
                            context.receive(
                                typeTranslator(typeAnalysis.type(sender)),
                                event.send.host
                            )
                        )
                        receiveBuilder.addStatement(
                            "throw %T(%S)",
                            runtimeErrorClass,
                            "Commitment Cleartext : received different values"
                        )
                        receiveBuilder.endControlFlow()
                    }
                }

                else -> {
                    var cleartextInputEvents: Set<CommunicationEvent> =
                        events.getProjectionReceives(projection, Commitment.INPUT)

                    if (cleartextInputEvents.isNotEmpty()) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            clearTextTemp,
                            context.receive(
                                typeTranslator(typeAnalysis.type(sender)),
                                cleartextInputEvents.first().send.host
                            )
                        )
                        cleartextInputEvents = cleartextInputEvents.minusElement(cleartextInputEvents.first())
                    }

                    // receive from the rest of the hosts and compare against clearTextValue
                    for (event in cleartextInputEvents) {

                        // check to make sure that you got the same data from all hosts
                        receiveBuilder.beginControlFlow(
                            "if (%N != %L)",
                            clearTextTemp,
                            context.receive(
                                typeTranslator(typeAnalysis.type(sender)),
                                event.send.host
                            )
                        )
                        receiveBuilder.addStatement(
                            "throw %T(%S)",
                            runtimeErrorClass,
                            "Commitment Cleartext : received different values"
                        )
                        receiveBuilder.endControlFlow()
                    }

                    // create commitment
                    receiveBuilder.addStatement(
                        "val %N = %N.commitment()",
                        commitTemp,
                        clearTextTemp
                    )

                    for (hashHost in hashHosts) {
                        receiveBuilder.addStatement(
                            "%L",
                            context.send(
                                CodeBlock.of(
                                    "%L",
                                    commitTemp
                                ),
                                hashHost
                            )
                        )
                    }
                }
            }
        }
        return receiveBuilder.build()
    }
}
