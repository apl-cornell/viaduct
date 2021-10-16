package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode

class CommitmentDispatchCodeGenerator(
    val host: Host,
    private val commitmentCreatorGenerator: CodeGenerator,
    private val commitmentHolderGenerator: CodeGenerator
) : CodeGenerator {

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        when (protocol) {
            is Commitment -> {
                if (host == protocol.cleartextHost) {
                    commitmentCreatorGenerator.guard(protocol, expr)
                } else {
                    commitmentHolderGenerator.guard(protocol, expr)
                }
            }
            else -> throw CodeGenerationError("Commitment generator dispatcher: got non-commitment protocol")
        }

    override fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock =
        when (protocol) {
            is Commitment -> {
                if (host == protocol.cleartextHost) {
                    commitmentCreatorGenerator.simpleStatement(protocol, stmt)
                } else {
                    commitmentHolderGenerator.simpleStatement(protocol, stmt)
                }
            }
            else -> throw CodeGenerationError("Commitment generator dispatcher: got non-commitment protocol")
        }

    override fun send(
        sendingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock =
        when (sendProtocol) {
            is Commitment -> {
                if (host == sendProtocol.cleartextHost) {
                    commitmentCreatorGenerator.send(sendingHost, sender, sendProtocol, receiveProtocol, events)
                } else {
                    commitmentHolderGenerator.send(sendingHost, sender, sendProtocol, receiveProtocol, events)
                }
            }
            else -> throw CodeGenerationError("Commitment generator dispatcher: got non-commitment protocol")
        }

    override fun receive(
        receivingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock =
        when (receiveProtocol) {
            is Commitment -> {
                if (host == receiveProtocol.cleartextHost) {
                    commitmentCreatorGenerator.receive(receivingHost, sender, sendProtocol, receiveProtocol, events)
                } else {
                    commitmentHolderGenerator.receive(receivingHost, sender, sendProtocol, receiveProtocol, events)
                }
            }
            else -> throw CodeGenerationError("Commitment generator dispatcher: got non-commitment protocol")
        }
}
