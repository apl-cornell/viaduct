package io.github.aplcornell.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.aplcornell.viaduct.selection.ProtocolCommunication
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.ObjectTypeNode
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.types.ObjectType
import io.github.aplcornell.viaduct.syntax.types.ValueType

interface CodeGenerator {
    fun kotlinType(protocol: Protocol, sourceType: ValueType): TypeName

    fun kotlinType(protocol: Protocol, sourceType: ObjectType): TypeName

    fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock

    fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock

    fun constructorCall(
        protocol: Protocol,
        objectType: ObjectTypeNode,
        arguments: Arguments<AtomicExpressionNode>,
    ): CodeBlock

    fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock

    fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication,
    ): CodeBlock

    fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication,
    ): CodeBlock

    fun setup(protocol: Protocol): Iterable<PropertySpec>
}

/** Combines code generators for different protocols into one generator that can handle all protocols. */
fun Iterable<Pair<Set<ProtocolName>, CodeGenerator>>.unions(): CodeGenerator = object : CodeGeneratorDispatcher() {
    private val codeGenerators: Map<ProtocolName, CodeGenerator> =
        this@unions.flatMap { it.first.map { name -> name to it.second } }.toMap()

    override fun generatorFor(protocol: Protocol): CodeGenerator =
        // TODO: more specific error message if no code generator exists for protocol.
        codeGenerators.getValue(protocol.protocolName)
}
