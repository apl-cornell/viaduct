package io.github.apl_cornell.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import io.github.apl_cornell.viaduct.selection.ProtocolCommunication
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.ObjectTypeNode
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.UpdateNode

interface CodeGenerator {
    fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock

    fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock

    fun constructorCall(
        protocol: Protocol,
        objectType: ObjectTypeNode,
        arguments: Arguments<AtomicExpressionNode>
    ): CodeBlock

    fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock

    fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock

    fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock

    fun setup(protocol: Protocol): Iterable<PropertySpec>
}

/** Combines code generators for different protocols into one generator that can handle all protocols. */
fun Iterable<Pair<Set<ProtocolName>, CodeGenerator>>.unions(): CodeGenerator = object : CodeGenerator {
    private val codeGenerators: Map<ProtocolName, CodeGenerator> =
        this@unions.flatMap { it.first.map { name -> name to it.second } }.toMap()

    private fun generatorFor(protocol: Protocol): CodeGenerator =
        // TODO: more specific error message if no code generator exists for protocol.
        codeGenerators.getValue(protocol.protocolName)

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        generatorFor(protocol).guard(protocol, expr)

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock = generatorFor(protocol).exp(protocol, expr)

    override fun constructorCall(
        protocol: Protocol,
        objectType: ObjectTypeNode,
        arguments: Arguments<AtomicExpressionNode>
    ): CodeBlock = generatorFor(protocol).constructorCall(protocol, objectType, arguments)

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock = generatorFor(protocol).update(protocol, stmt)

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock = generatorFor(sendProtocol).send(sender, sendProtocol, receiveProtocol, events)

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock = generatorFor(receiveProtocol).receive(sender, sendProtocol, receiveProtocol, events)

    override fun setup(protocol: Protocol): Iterable<PropertySpec> = generatorFor(protocol).setup(protocol)
}
