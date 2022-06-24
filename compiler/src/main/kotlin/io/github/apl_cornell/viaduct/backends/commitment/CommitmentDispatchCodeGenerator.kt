package io.github.apl_cornell.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.apl_cornell.viaduct.codegeneration.CodeGenerator
import io.github.apl_cornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.selection.ProtocolCommunication
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.SimpleStatementNode
import io.github.apl_cornell.viaduct.syntax.types.ObjectType
import io.github.apl_cornell.viaduct.syntax.types.ValueType

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

    override fun kotlinType(protocol: Protocol, sourceType: ValueType): TypeName =
        generatorFor(protocol).kotlinType(protocol, sourceType)

    override fun kotlinType(protocol: Protocol, sourceType: ObjectType): TypeName =
        generatorFor(protocol).kotlinType(protocol, sourceType)

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        generatorFor(protocol).guard(protocol, expr)

    override fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock =
        generatorFor(protocol).simpleStatement(protocol, stmt)

    override fun argument(protocol: Protocol, argument: FunctionArgumentNode): CodeBlock =
        generatorFor(protocol).argument(protocol, argument)

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
