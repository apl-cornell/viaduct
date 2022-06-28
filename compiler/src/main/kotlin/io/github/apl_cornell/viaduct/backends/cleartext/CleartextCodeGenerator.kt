package io.github.apl_cornell.viaduct.backends.cleartext

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import io.github.apl_cornell.viaduct.analysis.NameAnalysis
import io.github.apl_cornell.viaduct.analysis.TypeAnalysis
import io.github.apl_cornell.viaduct.codegeneration.AbstractCodeGenerator
import io.github.apl_cornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.codegeneration.UnsupportedOperatorException
import io.github.apl_cornell.viaduct.codegeneration.receiveReplicated
import io.github.apl_cornell.viaduct.codegeneration.typeTranslator
import io.github.apl_cornell.viaduct.codegeneration.valueClass
import io.github.apl_cornell.viaduct.runtime.EquivocationException
import io.github.apl_cornell.viaduct.runtime.commitment.Commitment
import io.github.apl_cornell.viaduct.runtime.commitment.Committed
import io.github.apl_cornell.viaduct.selection.CommunicationEvent
import io.github.apl_cornell.viaduct.selection.ProtocolCommunication
import io.github.apl_cornell.viaduct.syntax.BinaryOperator
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolProjection
import io.github.apl_cornell.viaduct.syntax.UnaryOperator
import io.github.apl_cornell.viaduct.syntax.datatypes.Modify
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LiteralNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.UpdateNode
import io.github.apl_cornell.viaduct.syntax.operators.Maximum
import io.github.apl_cornell.viaduct.syntax.operators.Minimum
import io.github.apl_cornell.viaduct.syntax.types.MutableCellType
import io.github.apl_cornell.viaduct.syntax.types.VectorType

class CleartextCodeGenerator(context: CodeGeneratorContext) : AbstractCodeGenerator(context) {
    private val typeAnalysis = TypeAnalysis.get(context.program)
    private val nameAnalysis = NameAnalysis.get(context.program)

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        cleartextExp(protocol, expr)

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> value(expr.value)

            is OperatorApplicationNode -> {
                when (expr.operator) {
                    Minimum ->
                        CodeBlock.of(
                            "%M(%L, %L)",
                            MemberName("kotlin.math", "min"),
                            exp(protocol, expr.arguments[0]),
                            exp(protocol, expr.arguments[1])
                        )
                    Maximum ->
                        CodeBlock.of(
                            "%M(%L, %L)",
                            MemberName("kotlin.math", "max"),
                            exp(protocol, expr.arguments[0]),
                            exp(protocol, expr.arguments[1])
                        )
                    is UnaryOperator ->
                        CodeBlock.of(
                            "%L%L",
                            expr.operator.toString(),
                            exp(protocol, expr.arguments[0])
                        )
                    is BinaryOperator ->
                        CodeBlock.of(
                            "%L %L %L",
                            exp(protocol, expr.arguments[0]),
                            expr.operator,
                            exp(protocol, expr.arguments[1])
                        )
                    else -> throw UnsupportedOperatorException(protocol, expr)
                }
            }

            is InputNode ->
                CodeBlock.of(
                    "(runtime.input(%T) as %T).value",
                    expr.type.value::class,
                    expr.type.value.valueClass
                )

            else -> super.exp(protocol, expr)
        }

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is MutableCellType ->
                when (stmt.update.value) {
                    is Modify ->
                        CodeBlock.of(
                            "%1N.set(%1N.get() %2L %3L)",
                            context.kotlinName(stmt.variable.value),
                            stmt.update.value.operator,
                            exp(protocol, stmt.arguments[0])
                        )

                    else -> super.update(protocol, stmt)
                }

            is VectorType ->
                when (stmt.update.value) {
                    is Modify ->
                        CodeBlock.of(
                            "%N[%L] %L %L",
                            context.kotlinName(stmt.variable.value),
                            cleartextExp(protocol, stmt.arguments[0]),
                            stmt.update.value.name,
                            exp(protocol, stmt.arguments[1])
                        )

                    else -> super.update(protocol, stmt)
                }

            else -> super.update(protocol, stmt)
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
                if (sender.value is InputNode)
                    sendBuilder.addStatement(
                        "%L",
                        context.send(
                            CodeBlock.of("%N", context.kotlinName(sender.name.value, sendProtocol)),
                            event.recv.host
                        )
                    )
                else
                    sendBuilder.addStatement("%L", context.send(exp(sendProtocol, sender.value), event.recv.host))
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
        val clearTextTemp = context.newTemporary("clearTextTemp")
        val clearTextCommittedTemp = context.newTemporary("cleartextCommittedTemp")
        if (sendProtocol != receiveProtocol) {
            val projection = ProtocolProjection(receiveProtocol, context.host)
            val cleartextInputs = events.getProjectionReceives(
                projection,
                Plaintext.INPUT
            )

            val cleartextCommitmentInputs =
                events.getProjectionReceives(projection, Plaintext.CLEARTEXT_COMMITMENT_INPUT)

            val hashCommitmentInputs =
                events.getProjectionReceives(projection, Plaintext.HASH_COMMITMENT_INPUT)

            when {
                cleartextInputs.isNotEmpty() && cleartextCommitmentInputs.isEmpty() &&
                    hashCommitmentInputs.isEmpty() -> {

                    receiveBuilder.addStatement(
                        "val %L = %L",
                        clearTextTemp,
                        receiveReplicated(
                            sender,
                            cleartextInputs,
                            context,
                            typeAnalysis
                        )
                    )

                    // calculate set of hosts with whom [receivingHost] needs to check for equivocation
                    val hostsToCheckWith: List<Host> =
                        events
                            .filter { event ->
                                // remove events where receiving host is not receiving plaintext data
                                event.recv.id == Plaintext.INPUT &&

                                    // remove events where a host is sending data to themselves
                                    event.send.host != event.recv.host &&

                                    // remove events where [receivingHost] is the sender of the data
                                    event.send.host != context.host
                            }
                            // of events matching above criteria, get set of data receivers
                            .map { event -> event.recv.host }
                            // remove [receivingHost] from the set of hosts with whom [receivingHost] needs to
                            // check for equivocation
                            .filter { host -> host != context.host }
                            .sorted()

                    for (host in hostsToCheckWith)
                        receiveBuilder.addStatement("%L", context.send(CodeBlock.of(clearTextTemp), host))

                    for (host in hostsToCheckWith) {
                        receiveBuilder.addStatement(
                            "%T.%N(%N, %L, %L, %L)",
                            EquivocationException::class,
                            "assertEquals",
                            clearTextTemp,
                            context.codeOf(cleartextInputs.first().send.host),
                            context.receive(typeTranslator(typeAnalysis.type(sender)), host),
                            context.codeOf(host)
                        )
                    }
                    receiveBuilder.addStatement(
                        "val %N = %N",
                        context.kotlinName(sender.name.value, receiveProtocol),
                        clearTextTemp
                    )

                    return receiveBuilder.build()
                }

                // commitment opening
                cleartextInputs.isEmpty() && cleartextCommitmentInputs.isNotEmpty() &&
                    hashCommitmentInputs.isNotEmpty() -> {

                    // sanity check, only open one commitment at once
                    if (cleartextCommitmentInputs.size != 1) {
                        throw IllegalArgumentException("Received multiple commitments to open.")
                    }

                    // receive declassified commitment from the hash holder
                    receiveBuilder.addStatement(
                        "val %N = %L",
                        clearTextCommittedTemp,
                        context.receive(
                            Committed::class.asClassName().parameterizedBy(
                                typeTranslator((typeAnalysis.type(sender)))
                            ),
                            cleartextCommitmentInputs.first().send.host
                        )
                    )

                    for (hashSendEvent in hashCommitmentInputs) {
                        receiveBuilder.addStatement(
                            "%L.%L(%N)",
                            context.receive(
                                Commitment::class.asClassName().parameterizedBy(
                                    typeTranslator((typeAnalysis.type(sender)))
                                ),
                                hashSendEvent.send.host
                            ),
                            "open",
                            clearTextCommittedTemp
                        )
                    }

                    receiveBuilder.addStatement(
                        "val %N = %L.%N",
                        context.kotlinName(sender.name.value, receiveProtocol),
                        clearTextCommittedTemp,
                        "value"
                    )
                }

                else ->
                    throw IllegalArgumentException("Received both commitment to open and cleartext value.")
            }
        }
        return receiveBuilder.build()
    }
}
