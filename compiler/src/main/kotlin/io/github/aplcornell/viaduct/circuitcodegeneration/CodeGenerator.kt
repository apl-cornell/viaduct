package io.github.aplcornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.aplcornell.viaduct.syntax.HasSourceLocation
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.circuit.ArrayTypeNode
import io.github.aplcornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.aplcornell.viaduct.syntax.types.ValueType

class Argument(
    val value: CodeBlock,
    val type: ArrayTypeNode,
    val protocol: Protocol,
    override val sourceLocation: SourceLocation,
) : HasSourceLocation

interface CodeGenerator {
    fun paramType(protocol: Protocol, sourceType: ValueType): TypeName

    fun storageType(protocol: Protocol, sourceType: ValueType): TypeName

    /**
     * Generates code for the body of circuit [circuitDeclaration].
     * @param outParams The list of names of Out boxes in which return values are to be stored.
     * */
    fun circuitBody(
        protocol: Protocol,
        circuitDeclaration: CircuitDeclarationNode,
        outParams: List<CodeBlock>,
    ): CodeBlock

    /**
     * Generates code for importing values into [protocol], and the names associated with imported results.
     * @param protocol The protocol values are being imported to.
     * @param arguments The arguments to be imported.
     * @return (codeBlock, names) such that codeBlock is the code which imports values, names is the list of names
     * associated with the results.
     */
    fun import(
        protocol: Protocol,
        arguments: List<Argument>,
    ): Pair<CodeBlock, List<CodeBlock>>

    /**
     * Generates code for exporting values from [protocol], and the names associated with exported results.
     * @param protocol The protocol values are being exported from.
     * @param arguments The arguments to be exported.
     * @return (codeBlock, names) such that codeBlock is the code which exports values, names is the list of names
     * associated with the results.
     */
    fun export(
        protocol: Protocol,
        arguments: List<Argument>,
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
