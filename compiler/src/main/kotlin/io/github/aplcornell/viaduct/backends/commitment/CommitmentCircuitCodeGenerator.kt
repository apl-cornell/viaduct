package io.github.aplcornell.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import io.github.aplcornell.viaduct.circuitcodegeneration.AbstractCodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.Argument
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.circuitcodegeneration.UnsupportedCommunicationException
import io.github.aplcornell.viaduct.circuitcodegeneration.typeTranslator
import io.github.aplcornell.viaduct.runtime.commitment.Committed
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.runtime.commitment.Commitment as CommitmentValue

/**
 * Backend code generator for the commitment protocol for the circuit IR.
 *
 * Throws an UnsupportedCommunicationException when used in an input program as a computation protocol.
 * This is because the commitment protocol is only a storage format and not a computation protocol.
 */
class CommitmentCircuitCodeGenerator(context: CodeGeneratorContext) : AbstractCodeGenerator(context) {
    override fun paramType(protocol: Protocol, sourceType: ValueType): TypeName {
        require(protocol is Commitment)
        return when (context.host) {
            protocol.cleartextHost -> (Committed::class).asTypeName().parameterizedBy(typeTranslator(sourceType))
            in protocol.hashHosts -> (CommitmentValue::class).asTypeName().parameterizedBy(typeTranslator(sourceType))
            else -> throw IllegalStateException()
        }
    }

    override fun storageType(protocol: Protocol, sourceType: ValueType): TypeName {
        return super.storageType(protocol, sourceType)
    }

    override fun import(protocol: Protocol, arguments: List<Argument>, ): Pair<CodeBlock, List<CodeBlock>> {
        throw UnsupportedCommunicationException(arguments.first().protocol, protocol, arguments.first().sourceLocation)
    }

    override fun export(protocol: Protocol, arguments: List<Argument>): Pair<CodeBlock, List<CodeBlock>> {
        throw UnsupportedCommunicationException(arguments.first().protocol, protocol, arguments.first().sourceLocation)
    }
}
