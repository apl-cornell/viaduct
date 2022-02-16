package edu.cornell.cs.apl.viaduct.backends.commitment

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.codegeneration.AbstractCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.receiveReplicated
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode

private const val commitmentPackage = "edu.cornell.cs.apl.viaduct.runtime.commitment"
private val Committed = ClassName(commitmentPackage, "Committed")

internal class CommitmentCreatorGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    private val typeAnalysis = TypeAnalysis.get(context.program)

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock = exp(protocol, expr)

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(
                "%T.%N(%L)",
                Committed,
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
                if (event.send.host != event.recv.host) {
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
                            context.kotlinName(sender.temporary.value, receiveProtocol),
                            receiveReplicated(
                                sender,
                                sendProtocol,
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
                        context.kotlinName(sender.temporary.value, receiveProtocol),
                        Committed,
                        receiveReplicated(
                            sender,
                            sendProtocol,
                            cleartextInputEvents,
                            context,
                            typeAnalysis
                        )
                    )

                    // create commitment
                    receiveBuilder.addStatement(
                        "val %N = %N.%M()",
                        commitmentTemp,
                        context.kotlinName(sender.temporary.value, receiveProtocol),
                        MemberName(Committed.nestedClass("Companion"), "commitment")
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
