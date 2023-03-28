package io.github.aplcornell.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.github.aplcornell.viaduct.analysis.TypeAnalysis
import io.github.aplcornell.viaduct.codegeneration.AbstractCodeGenerator
import io.github.aplcornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.codegeneration.receiveReplicated
import io.github.aplcornell.viaduct.codegeneration.typeTranslator
import io.github.aplcornell.viaduct.runtime.commitment.Committed
import io.github.aplcornell.viaduct.selection.CommunicationEvent
import io.github.aplcornell.viaduct.selection.ProtocolCommunication
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolProjection
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.LiteralNode
import io.github.aplcornell.viaduct.syntax.types.ValueType

internal class CommitmentCreatorGenerator(
    context: CodeGeneratorContext,
) : AbstractCodeGenerator(context) {
    private val typeAnalysis = context.program.analyses.get<TypeAnalysis>()

    override fun kotlinType(protocol: Protocol, sourceType: ValueType): TypeName =
        (Committed::class).asTypeName().parameterizedBy(typeTranslator(sourceType))

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        cleartextExp(protocol, expr)

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(
                "%T.%N(%L)",
                Committed::class,
                "fake",
                value(expr.value),
            )

            else -> super.exp(protocol, expr)
        }

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication,
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()
        if (sendProtocol != receiveProtocol) {
            val relevantEvents: Set<CommunicationEvent> =
                events.getProjectionSends(ProtocolProjection(sendProtocol, context.host))

            for (event in relevantEvents) {
                // send temporary containing hash value
                sendBuilder.addStatement(
                    "%L",
                    context.send(
                        CodeBlock.of("%N", context.kotlinName(sender.name.value, sendProtocol)),
                        event.recv.host,
                    ),
                )
            }
        }
        return sendBuilder.build()
    }

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication,
    ): CodeBlock {
        val receiveBuilder = CodeBlock.builder()
        val projection = ProtocolProjection(receiveProtocol, context.host)
        val hashHosts: Set<Host> = (projection.protocol as Commitment).hashHosts
        val commitmentTemp = context.newTemporary("commitment")
        if (sendProtocol != receiveProtocol) {
            when {
                events.any { event -> event.recv.id == Commitment.CLEARTEXT_INPUT } -> {
                    val relevantEvents =
                        events.getHostReceives(
                            projection.host,
                            Commitment.CLEARTEXT_INPUT,
                        )

                    if (relevantEvents.isNotEmpty()) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            context.kotlinName(sender.name.value, receiveProtocol),
                            receiveReplicated(
                                sender,
                                relevantEvents,
                                context,
                                typeAnalysis,
                            ),
                        )
                    }
                }

                else -> {
                    val cleartextInputEvents =
                        events.getProjectionReceives(
                            projection,
                            Commitment.INPUT,
                        )

                    receiveBuilder.addStatement(
                        "val %N = %T(%L)",
                        context.kotlinName(sender.name.value, receiveProtocol),
                        Committed::class,
                        receiveReplicated(
                            sender,
                            cleartextInputEvents,
                            context,
                            typeAnalysis,
                        ),
                    )

                    // create commitment
                    receiveBuilder.addStatement(
                        "val %N = %N.%M()",
                        commitmentTemp,
                        context.kotlinName(sender.name.value, receiveProtocol),
                        MemberName(Committed.Companion::class.asClassName(), "commitment"),
                    )

                    for (hashHost in hashHosts) {
                        receiveBuilder.addStatement(
                            "%L",
                            context.send(
                                CodeBlock.of(
                                    "%L",
                                    commitmentTemp,
                                ),
                                hashHost,
                            ),
                        )
                    }
                }
            }
        }
        return receiveBuilder.build()
    }
}
