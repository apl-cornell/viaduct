package io.github.aplcornell.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.aplcornell.viaduct.selection.ProtocolCommunication
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.ObjectTypeNode
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.types.ObjectType
import io.github.aplcornell.viaduct.syntax.types.ValueType

/** Dispatches to different [CodeGenerator]s based on [Protocol]. */
abstract class CodeGeneratorDispatcher : CodeGenerator {
    /** Returns the [CodeGenerator] to use for [protocol]. */
    protected abstract fun generatorFor(protocol: Protocol): CodeGenerator

    final override fun kotlinType(
        protocol: Protocol,
        sourceType: ValueType,
    ): TypeName = generatorFor(protocol).kotlinType(protocol, sourceType)

    final override fun kotlinType(
        protocol: Protocol,
        sourceType: ObjectType,
    ): TypeName = generatorFor(protocol).kotlinType(protocol, sourceType)

    final override fun guard(
        protocol: Protocol,
        expr: AtomicExpressionNode,
    ): CodeBlock = generatorFor(protocol).guard(protocol, expr)

    final override fun exp(
        protocol: Protocol,
        expr: ExpressionNode,
    ): CodeBlock = generatorFor(protocol).exp(protocol, expr)

    final override fun constructorCall(
        protocol: Protocol,
        objectType: ObjectTypeNode,
        arguments: Arguments<AtomicExpressionNode>,
    ): CodeBlock = generatorFor(protocol).constructorCall(protocol, objectType, arguments)

    final override fun update(
        protocol: Protocol,
        stmt: UpdateNode,
    ): CodeBlock = generatorFor(protocol).update(protocol, stmt)

    final override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication,
    ): CodeBlock = generatorFor(sendProtocol).send(sender, sendProtocol, receiveProtocol, events)

    final override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication,
    ): CodeBlock = generatorFor(receiveProtocol).receive(sender, sendProtocol, receiveProtocol, events)

    final override fun setup(protocol: Protocol): Iterable<PropertySpec> = generatorFor(protocol).setup(protocol)
}
