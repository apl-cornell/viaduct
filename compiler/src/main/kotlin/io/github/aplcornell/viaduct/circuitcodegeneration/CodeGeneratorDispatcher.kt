package io.github.aplcornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.aplcornell.viaduct.syntax.types.ValueType

/** Dispatches to different [CodeGenerator]s based on [Protocol]. */
abstract class CodeGeneratorDispatcher : CodeGenerator {
    /** Returns the [CodeGenerator] to use for [protocol]. */
    protected abstract fun generatorFor(protocol: Protocol): CodeGenerator

    final override fun paramType(protocol: Protocol, sourceType: ValueType): TypeName =
        generatorFor(protocol).paramType(protocol, sourceType)

    final override fun storageType(protocol: Protocol, sourceType: ValueType): TypeName =
        generatorFor(protocol).paramType(protocol, sourceType)

    final override fun circuitBody(
        protocol: Protocol,
        circuitDeclaration: CircuitDeclarationNode,
        outParams: List<CodeBlock>,
    ): CodeBlock =
        generatorFor(protocol).circuitBody(protocol, circuitDeclaration, outParams)

    final override fun import(
        protocol: Protocol,
        arguments: List<Argument>,
    ) = generatorFor(protocol).import(protocol, arguments)

    final override fun export(
        protocol: Protocol,
        arguments: List<Argument>,
    ) = generatorFor(protocol).export(protocol, arguments)

    final override fun setup(protocol: Protocol): Iterable<PropertySpec> =
        generatorFor(protocol).setup(protocol)
}
