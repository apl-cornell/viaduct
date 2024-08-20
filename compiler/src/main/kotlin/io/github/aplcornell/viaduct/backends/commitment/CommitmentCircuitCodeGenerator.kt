package io.github.aplcornell.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.github.aplcornell.viaduct.backends.cleartext.Local
import io.github.aplcornell.viaduct.backends.cleartext.Replication
import io.github.aplcornell.viaduct.circuitcodegeneration.AbstractCodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.Argument
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.circuitcodegeneration.UnsupportedCommunicationException
import io.github.aplcornell.viaduct.runtime.commitment.Committed
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.runtime.commitment.Commitment as CommittmentValue

class CommitmentCircuitCodeGenerator(context: CodeGeneratorContext) : AbstractCodeGenerator(context) {
    private fun createCommitment(
        source: Protocol,
        target: Protocol,
        argument: Argument,
        builder: CodeBlock.Builder,
    ): CodeBlock {
        require(context.host in source.hosts + target.hosts)
        require(source is Local && source.hosts.size == 1 && source.host in source.hosts)
        require(target is Commitment)
        if (target.cleartextHost != source.host || target.cleartextHost in target.hashHosts) {
            throw UnsupportedCommunicationException(source, target, argument.sourceLocation)
        }
        val argType = storageType(argument.protocol, argument.type)
        val sendingHost = target.cleartextHost
        val receivingHosts = target.hashHosts
        return when (context.host) {
            sendingHost -> {
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
                CodeBlock.of("%N", tempName1)
            }

            in receivingHosts -> {
                val tempName3 = context.newTemporary("CommitTemp")
                builder.addStatement(
                    "val %N = %L",
                    tempName3,
                    context.receive((CommittmentValue::class).asTypeName().parameterizedBy(argType), source.host),
                )
                CodeBlock.of("%N", tempName3)
            }

            else -> throw IllegalStateException()
        }
    }

    private fun openCommitment(
        source: Protocol,
        target: Protocol,
        argument: Argument,
        builder: CodeBlock.Builder,
    ): CodeBlock {
        require(source is Commitment)
        require(target is Replication)
        require(context.host in source.hosts + target.hosts)
        require(source.cleartextHost !in source.hashHosts)
        if (source.hashHosts != target.hosts) {
            throw UnsupportedCommunicationException(source, target, argument.sourceLocation)
        }
        val receivingHosts = target.hosts
        return when (context.host) {
            source.cleartextHost -> {
                receivingHosts.forEach {
                    builder.addStatement("%L", context.send(CodeBlock.of("%N", argument.value), it))
                }
                CodeBlock.of("")
            }
            in receivingHosts -> {
//                TODO("Receive the committed object and open the argument Commitment object")
                val tempName1 = context.newTemporary("CommitTemp")
                builder.addStatement(
                    "val %N = %L",
                    tempName1,
                    context.receive((Committed::class).asTypeName(), source.cleartextHost),
                )
                val tempName2 = context.newTemporary("CommitTemp")
                builder.addStatement(
                    "val %N = %L",
                    tempName2,
                    argument.value,
                )
                val tempName3 = context.newTemporary("CommitTemp")
                builder.addStatement(
                    "val %N = %N.%M(%N)",
                    tempName3,
                    tempName2,
                    MemberName(CommittmentValue::class.asClassName(), "open"),
                    tempName1,
                )
                CodeBlock.of("%N", tempName3)
            }
            else -> throw IllegalStateException()
        }
    }

    override fun import(
        protocol: Protocol,
        arguments: List<Argument>,
    ): Pair<CodeBlock, List<CodeBlock>> {
        require(protocol is Commitment)
        val builder = CodeBlock.builder()
        val values = arguments.map { arg ->
            when (arg.protocol) {
                is Local -> {
                    if (context.host in protocol.hosts + arg.protocol.hosts) {
                        createCommitment(arg.protocol, protocol, arg, builder)
                    } else {
                        CodeBlock.of("")
                    }
                }
                is Commitment -> {
                    if (context.host in protocol.hosts + arg.protocol.hosts) {
                        builder.addStatement("%L", arg.value)
                        arg.value
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
        require(protocol is Commitment)
        val builder = CodeBlock.builder()
        val values = arguments.map { arg ->
            when (arg.protocol) {
                is Commitment -> {
                    if (context.host in protocol.hosts + arg.protocol.hosts) {
                        builder.addStatement("%L", arg.value)
                        arg.value
                    } else {
                        CodeBlock.of("")
                    }
                }
                is Replication -> {
                    openCommitment(protocol, arg.protocol, arg, builder)
                }
                else -> throw UnsupportedCommunicationException(arg.protocol, protocol, arg.sourceLocation)
            }
        }
        return Pair(builder.build(), values)
    }
}

// Import from local to commitment: If cleartextHost, create a commitment object and send. If hashHost, receive the commitment object
// Export from commitment to commitment: Do nothing
// Import from commitment to commitment: Do nothing
// Export from commitment to Replication: If cleartextHost, send the secret. If hashHost, receive the secret and open the commitment object
