package edu.cornell.cs.apl.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode

class CommitmentDispatchCodeGenerator(
    val context: CodeGeneratorContext
) : CodeGenerator {
    private val commitmentCreatorGenerator = CommitmentCreatorGenerator(context)
    private val commitmentHolderGenerator = CommitmentHolderGenerator(context)

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        when (protocol) {
            is Commitment -> {
                if (context.host == protocol.cleartextHost) {
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
                if (context.host == protocol.cleartextHost) {
                    commitmentCreatorGenerator.simpleStatement(protocol, stmt)
                } else {
                    commitmentHolderGenerator.simpleStatement(protocol, stmt)
                }
            }
            else -> throw CodeGenerationError("Commitment generator dispatcher: got non-commitment protocol")
        }

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock =
        when (sendProtocol) {
            is Commitment -> {
                if (context.host == sendProtocol.cleartextHost) {
                    commitmentCreatorGenerator.send(sender, sendProtocol, receiveProtocol, events)
                } else {
                    commitmentHolderGenerator.send(sender, sendProtocol, receiveProtocol, events)
                }
            }
            else -> throw CodeGenerationError("Commitment generator dispatcher: got non-commitment protocol")
        }

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock =
        when (receiveProtocol) {
            is Commitment -> {
                if (context.host == receiveProtocol.cleartextHost) {
                    commitmentCreatorGenerator.receive(sender, sendProtocol, receiveProtocol, events)
                } else {
                    commitmentHolderGenerator.receive(sender, sendProtocol, receiveProtocol, events)
                }
            }
            else -> throw CodeGenerationError("Commitment generator dispatcher: got non-commitment protocol")
        }
}
