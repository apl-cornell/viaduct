package io.github.apl_cornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.apl_cornell.viaduct.selection.ProtocolCommunication
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.circuit.ArrayType
import io.github.apl_cornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.apl_cornell.viaduct.syntax.circuit.LetNode
import io.github.apl_cornell.viaduct.syntax.types.ValueType

/** Dispatches to different [CodeGenerator]s based on [Protocol]. */
abstract class CodeGeneratorDispatcher : CodeGenerator {
    /** Returns the [CodeGenerator] to use for [protocol]. */
    protected abstract fun generatorFor(protocol: Protocol): CodeGenerator

    final override fun kotlinType(protocol: Protocol, sourceType: ValueType): TypeName =
        generatorFor(protocol).kotlinType(protocol, sourceType)

    final override fun kotlinType(protocol: Protocol, sourceType: ArrayType): TypeName =
        generatorFor(protocol).kotlinType(protocol, sourceType)

    final override fun circuitBody(
        protocol: Protocol,
        host: Host,
        circuitDeclaration: CircuitDeclarationNode
    ): CodeBlock =
        generatorFor(protocol).circuitBody(protocol, host, circuitDeclaration)

    final override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock =
        generatorFor(sendProtocol).send(sender, sendProtocol, receiveProtocol, events)

    final override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock =
        generatorFor(receiveProtocol).receive(sender, sendProtocol, receiveProtocol, events)

    final override fun setup(protocol: Protocol): Iterable<PropertySpec> =
        generatorFor(protocol).setup(protocol)
}
