package edu.cornell.cs.apl.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.codegeneration.AbstractCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.UnsupportedOperatorException
import edu.cornell.cs.apl.viaduct.codegeneration.receiveReplicated
import edu.cornell.cs.apl.viaduct.codegeneration.typeTranslator
import edu.cornell.cs.apl.viaduct.runtime.commitment.Commitment
import edu.cornell.cs.apl.viaduct.runtime.commitment.Committed
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.backends.commitment.Commitment as CommitmentProtocol

internal class CommitmentHolderGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    private val typeAnalysis: TypeAnalysis = TypeAnalysis.get(context.program)

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        throw UnsupportedOperatorException(protocol, expr)

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(
                "%T.%N(%L).%M()",
                Committed::class,
                "fake",
                value(expr.value),
                MemberName(Committed.Companion::class.asClassName(), "commitment")
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

        // here, the interpreter checks for the available protocols, is this necessary here?
        val relevantEvents: List<CommunicationEvent> =
            events.getProjectionSends(
                ProtocolProjection(sendProtocol, context.host),
                CommitmentProtocol.OPEN_COMMITMENT_OUTPUT
            ).toList()

        for (event in relevantEvents) {
            if (event.send.host != event.recv.host) {
                sendBuilder.addStatement(
                    "%L",
                    context.send(
                        CodeBlock.of("%N", context.kotlinName(sender.name.value, sendProtocol)),
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
        if (sendProtocol != receiveProtocol) {
            when {
                events.any { event -> event.recv.id == CommitmentProtocol.CLEARTEXT_INPUT } -> {
                    val relevantEvents =
                        events.getHostReceives(
                            projection.host,
                            CommitmentProtocol.CLEARTEXT_INPUT
                        )

                    receiveBuilder.addStatement(
                        "val %N = %L",
                        context.kotlinName(sender.name.value, receiveProtocol),
                        receiveReplicated(
                            sender,
                            sendProtocol,
                            relevantEvents,
                            context,
                            typeAnalysis
                        ),
                    )
                }

                else -> { // create commitment
                    if (context.host !in sendProtocol.hosts) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            context.kotlinName(sender.name.value, receiveProtocol),
                            context.receive(
                                Commitment::class.asClassName().parameterizedBy(
                                    typeTranslator(typeAnalysis.type(sender))
                                ),
                                events.first().send.host
                            )
                        )
                    }
                }
            }
        }
        return receiveBuilder.build()
    }
}
