package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode

interface CodeGenerator {
    fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock

    fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock

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
}

/** Combines code generators for different protocols into one generator that can handle all protocols. */
fun Iterable<Pair<Set<ProtocolName>, CodeGenerator>>.unions(): CodeGenerator =
    object : CodeGenerator {
        private val codeGenerators: Map<ProtocolName, CodeGenerator> =
            this@unions.flatMap { it.first.map { name -> name to it.second } }.toMap()

        private fun generatorFor(protocol: Protocol): CodeGenerator =
            // TODO: more specific error message if no code generator exists for protocol.
            codeGenerators.getValue(protocol.protocolName)

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
            generatorFor(receiveProtocol).send(sender, sendProtocol, receiveProtocol, events)
    }
