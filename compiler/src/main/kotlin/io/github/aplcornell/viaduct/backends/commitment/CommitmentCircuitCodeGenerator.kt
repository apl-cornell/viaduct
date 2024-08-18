package io.github.aplcornell.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.github.aplcornell.viaduct.backends.cleartext.Local
import io.github.aplcornell.viaduct.circuitcodegeneration.AbstractCodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.Argument
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.circuitcodegeneration.UnsupportedCommunicationException
import io.github.aplcornell.viaduct.runtime.commitment.Committed
import io.github.aplcornell.viaduct.syntax.Protocol

class CommitmentCircuitCodeGenerator(context: CodeGeneratorContext) : AbstractCodeGenerator(context) {
    private fun move(
        source: Protocol,
        target: Protocol,
        argument: Argument,
        builder: CodeBlock.Builder
    ): CodeBlock {
        require(context.host in source.hosts + target.hosts)
        require(source is Local && source.hosts.size == 1 && source.host in source.hosts)
        require(target is Commitment)
        if (target.cleartextHost != source.host) {
            throw UnsupportedCommunicationException(source, target, argument.sourceLocation)
        }
        val argType = storageType(argument.protocol, argument.type)
        val receivingHosts = target.hashHosts
        return when (context.host) {
            in source.hosts -> {
                val tempName1 = context.newTemporary("CommitTemp")
                val tempName2 = context.newTemporary("CommitTemp")
                builder.addStatement(
                    "val %N = %T(%L)",
                    tempName1,
                    (Committed::class).asTypeName().parameterizedBy(argType),
                    argument.value,
                )
                builder.addStatement(
                    "val %N = %N.%M()",
                    tempName2,
                    tempName1,
                    MemberName(Committed.Companion::class.asClassName(), "commitment"),
                )
                receivingHosts.forEach {
                    builder.addStatement("%L", context.send(CodeBlock.of("%N", tempName2), it))
                }
                CodeBlock.of("%N", tempName2)
            }

            in receivingHosts -> {
                val tempName1 = context.newTemporary("CommitTemp")
                builder.addStatement(
                    "val %N = %L",
                    tempName1,
                    context.receive(argType, source.host),
                )
                CodeBlock.of("%N", tempName1)
            }

            else -> throw IllegalStateException()
        }
    }

    override fun import(
        protocol: Protocol,
        arguments: List<Argument>
    ): Pair<CodeBlock, List<CodeBlock>> {
        require(protocol is Commitment)
        val builder = CodeBlock.builder()
        val values = arguments.map { arg ->
            when (arg.protocol) {
                is Local -> {
                    if (context.host in protocol.hosts + arg.protocol.hosts) {
                        move(arg.protocol, protocol, arg, builder)
                    } else {
                        CodeBlock.of("")
                    }
                }

                else -> throw UnsupportedCommunicationException(arg.protocol, protocol, arg.sourceLocation)
            }
        }
        return Pair(builder.build(), values)
    }

    override fun export(protocol: Protocol, arguments: List<Argument>): Pair<CodeBlock, List<CodeBlock>> {
        TODO("Not yet implemented")
    }
}

