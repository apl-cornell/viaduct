package edu.cornell.cs.apl.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.codegeneration.AbstractCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.receiveReplicated
import edu.cornell.cs.apl.viaduct.runtime.commitment.Committed
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode

internal class CommitmentCreatorGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    private val typeAnalysis = TypeAnalysis.get(context.program)

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock = exp(protocol, expr)

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(
                "%T.%N(%L)",
                Committed::class,
                "fake",
                value(expr.value)
            )

            else -> super.exp(protocol, expr)
        }

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
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
                        CodeBlock.of("%L", context.kotlinName(sender.name.value, sendProtocol)),
                        event.recv.host
                    )
                )
            }
        }
        return sendBuilder.build()
    }

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
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
                            Commitment.CLEARTEXT_INPUT
                        )

                    if (relevantEvents.isNotEmpty()) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            context.kotlinName(sender.name.value, receiveProtocol),
                            receiveReplicated(
                                sender,
                                relevantEvents,
                                context,
                                typeAnalysis
                            )
                        )
                    }
                }

                else -> {
                    val cleartextInputEvents =
                        events.getProjectionReceives(
                            projection,
                            Commitment.INPUT
                        )

                    receiveBuilder.addStatement(
                        "val %N = %T(%L)",
                        context.kotlinName(sender.name.value, receiveProtocol),
                        Committed::class,
                        receiveReplicated(
                            sender,
                            cleartextInputEvents,
                            context,
                            typeAnalysis
                        )
                    )

                    // create commitment
                    receiveBuilder.addStatement(
                        "val %N = %N.%M()",
                        commitmentTemp,
                        context.kotlinName(sender.name.value, receiveProtocol),
                        MemberName(Committed.Companion::class.asClassName(), "commitment")
                    )

                    for (hashHost in hashHosts) {
                        receiveBuilder.addStatement(
                            "%L",
                            context.send(
                                CodeBlock.of(
                                    "%L",
                                    commitmentTemp
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
