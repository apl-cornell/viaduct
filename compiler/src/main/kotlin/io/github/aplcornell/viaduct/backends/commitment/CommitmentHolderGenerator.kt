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
import io.github.aplcornell.viaduct.runtime.commitment.Commitment
import io.github.aplcornell.viaduct.runtime.commitment.Committed
import io.github.aplcornell.viaduct.selection.CommunicationEvent
import io.github.aplcornell.viaduct.selection.ProtocolCommunication
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolProjection
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.LiteralNode
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.backends.commitment.Commitment as CommitmentProtocol

internal class CommitmentHolderGenerator(
    context: CodeGeneratorContext,
) : AbstractCodeGenerator(context) {
    private val typeAnalysis = context.program.analyses.get<TypeAnalysis>()

    override fun kotlinType(
        protocol: Protocol,
        sourceType: ValueType,
    ): TypeName = (Commitment::class).asTypeName().parameterizedBy(typeTranslator(sourceType))

    override fun exp(
        protocol: Protocol,
        expr: ExpressionNode,
    ): CodeBlock =
        when (expr) {
            is LiteralNode ->
                CodeBlock.of(
                    "%T.%N(%L).%M()",
                    Committed::class,
                    "fake",
                    value(expr.value),
                    MemberName(Committed.Companion::class.asClassName(), "commitment"),
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

        // here, the interpreter checks for the available protocols, is this necessary here?
        val relevantEvents: List<CommunicationEvent> =
            events.getProjectionSends(
                ProtocolProjection(sendProtocol, context.host),
                CommitmentProtocol.OPEN_COMMITMENT_OUTPUT,
            ).toList()

        for (event in relevantEvents) {
            sendBuilder.addStatement(
                "%L",
                context.send(
                    CodeBlock.of("%N", context.kotlinName(sender.name.value, sendProtocol)),
                    event.recv.host,
                ),
            )
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
        if (sendProtocol != receiveProtocol) {
            when {
                events.any { event -> event.recv.id == CommitmentProtocol.CLEARTEXT_INPUT } -> {
                    val relevantEvents =
                        events.getHostReceives(
                            projection.host,
                            CommitmentProtocol.CLEARTEXT_INPUT,
                        )

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

                else -> { // create commitment
                    if (context.host !in sendProtocol.hosts) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            context.kotlinName(sender.name.value, receiveProtocol),
                            context.receive(
                                Commitment::class.asClassName().parameterizedBy(
                                    typeTranslator(typeAnalysis.type(sender)),
                                ),
                                events.first().send.host,
                            ),
                        )
                    }
                }
            }
        }
        return receiveBuilder.build()
    }
}
