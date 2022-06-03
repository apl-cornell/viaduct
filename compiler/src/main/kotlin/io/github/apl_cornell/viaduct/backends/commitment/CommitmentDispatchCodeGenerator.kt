package io.github.apl_cornell.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import io.github.apl_cornell.viaduct.codegeneration.CodeGenerator
import io.github.apl_cornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.selection.ProtocolCommunication
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.SimpleStatementNode

class CommitmentDispatchCodeGenerator(
    val context: CodeGeneratorContext
) : CodeGenerator {
    private val commitmentCreatorGenerator = CommitmentCreatorGenerator(context)
    private val commitmentHolderGenerator = CommitmentHolderGenerator(context)

    private fun generatorFor(protocol: Protocol): CodeGenerator {
        require(protocol is Commitment)
        return if (context.host == protocol.cleartextHost)
            commitmentCreatorGenerator
        else
            commitmentHolderGenerator
    }

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        generatorFor(protocol).guard(protocol, expr)

    override fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock =
        generatorFor(protocol).simpleStatement(protocol, stmt)

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock =
        generatorFor(sendProtocol).send(sender, sendProtocol, receiveProtocol, events)

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock =
        generatorFor(receiveProtocol).receive(sender, sendProtocol, receiveProtocol, events)

    override fun setup(protocol: Protocol): Iterable<PropertySpec> = listOf()
}
