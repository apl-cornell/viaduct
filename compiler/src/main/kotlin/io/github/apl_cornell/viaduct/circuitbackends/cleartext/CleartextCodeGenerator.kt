package io.github.apl_cornell.viaduct.circuitbackends.cleartext

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import io.github.apl_cornell.viaduct.circuitcodegeneration.AbstractCodeGenerator
import io.github.apl_cornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.circuitcodegeneration.UnsupportedOperatorException
import io.github.apl_cornell.viaduct.selection.ProtocolCommunication
import io.github.apl_cornell.viaduct.syntax.BinaryOperator
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.UnaryOperator
import io.github.apl_cornell.viaduct.syntax.circuit.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.circuit.LetNode
import io.github.apl_cornell.viaduct.syntax.circuit.OperatorApplicationNode
import io.github.apl_cornell.viaduct.syntax.operators.Maximum
import io.github.apl_cornell.viaduct.syntax.operators.Minimum

class CleartextCodeGenerator(context: CodeGeneratorContext) : AbstractCodeGenerator(context) {
//    private val typeAnalysis = TypeAnalysis.get(context.program)
//    private val nameAnalysis = NameAnalysis.get(context.program)

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock = when (expr) {
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
        else -> super.exp(protocol, expr)
    }

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ) = CodeBlock.of("Send not yet implemented")
    /*: CodeBlock {
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
    }*/

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ) = CodeBlock.of("Receive not yet implemented")
    /*: CodeBlock {
    val receiveBuilder = CodeBlock.builder()
    val clearTextTemp = context.newTemporary("clearTextTemp")
    val clearTextCommittedTemp = context.newTemporary("cleartextCommittedTemp")
    if (sendProtocol != receiveProtocol) {
        val projection = ProtocolProjection(receiveProtocol, context.host)
        val cleartextInputs = events.getProjectionReceives(
            projection,
            Cleartext.INPUT
        )

        val cleartextCommitmentInputs =
            events.getProjectionReceives(projection, Cleartext.CLEARTEXT_COMMITMENT_INPUT)

        val hashCommitmentInputs =
            events.getProjectionReceives(projection, Cleartext.HASH_COMMITMENT_INPUT)

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
                            // remove events where receiving host is not receiving cleartext data
                            event.recv.id == Cleartext.INPUT &&

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
*/
}
