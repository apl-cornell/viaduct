package io.github.apl_cornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.circuit.ArrayTypeNode
import io.github.apl_cornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.apl_cornell.viaduct.syntax.types.ValueType

class Argument(
    val value: CodeBlock,
    val type: ArrayTypeNode,
    val protocol: Protocol,
    override val sourceLocation: SourceLocation
) : HasSourceLocation

interface CodeGenerator {
    fun paramType(protocol: Protocol, sourceType: ValueType): TypeName

    fun storageType(protocol: Protocol, sourceType: ValueType): TypeName

    /** Generates code for the body of circuit [circuitDeclaration]. */
    fun circuitBody(protocol: Protocol, circuitDeclaration: CircuitDeclarationNode): CodeBlock

    /**
     * Generates code for importing values from storage formats, and the names associated with imported results.
     * @param protocol The protocol values are being imported to.
     * @param valuesAndSources CodeBlocks which reference the values to be imported, and the protocols on which the
     * values are stored.
     * @return (codeBlock, names) such that codeBlock is the code which imports values, names is the list of names
     * associated with the results.
     *///TODO updateme
    fun import(
        protocol: Protocol,
        arguments: List<Argument>
    ): Pair<CodeBlock, List<CodeBlock>>

    fun export(
        protocol: Protocol,
        arguments: List<Argument>
    ): Pair<CodeBlock, List<CodeBlock>>

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
