package io.github.aplcornell.viaduct.backends.cleartext

import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.github.aplcornell.viaduct.circuitcodegeneration.AbstractCodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.Argument
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.circuitcodegeneration.UnsupportedCommunicationException
import io.github.aplcornell.viaduct.circuitcodegeneration.kotlinType
import io.github.aplcornell.viaduct.circuitcodegeneration.receiveExpected
import io.github.aplcornell.viaduct.circuitcodegeneration.receiveReplicated
import io.github.aplcornell.viaduct.circuitcodegeneration.typeTranslator
import io.github.aplcornell.viaduct.runtime.commitment.Commitment
import io.github.aplcornell.viaduct.runtime.commitment.Committed
import io.github.aplcornell.viaduct.syntax.BinaryOperator
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.UnaryOperator
import io.github.aplcornell.viaduct.syntax.circuit.OperatorNode
import io.github.aplcornell.viaduct.syntax.operators.Maximum
import io.github.aplcornell.viaduct.syntax.operators.Minimum
import io.github.aplcornell.viaduct.syntax.values.HostSetValue
import io.github.aplcornell.viaduct.backends.commitment.Commitment as CommitmentProtocol

class CleartextCircuitCodeGenerator(context: CodeGeneratorContext) : AbstractCodeGenerator(context) {

    override fun operatorApplication(protocol: Protocol, op: OperatorNode, arguments: List<CodeBlock>): CodeBlock =
        when (op.operator) {
            Minimum ->
                CodeBlock.of(
                    "%M(%L, %L)",
                    MemberName("kotlin.math", "min"),
                    arguments[0],
                    arguments[1],
                )

            Maximum ->
                CodeBlock.of(
                    "%M(%L, %L)",
                    MemberName("kotlin.math", "max"),
                    arguments[0],
                    arguments[1],
                )

            is UnaryOperator ->
                CodeBlock.of(
                    "%L%L",
                    op.operator.toString(),
                    arguments[0],
                )

            is BinaryOperator ->
                CodeBlock.of(
                    "%L %L %L",
                    arguments[0],
                    op.operator,
                    arguments[1],
                )

            else -> super.operatorApplication(protocol, op, arguments)
        }

    private fun receive(
        senders: Set<Host>,
        receivers: Set<Host>,
        argument: Argument,
    ): Pair<CodeBlock, CodeBlock> {
        require(context.host !in senders)
        require(context.host in receivers)
        val builder = CodeBlock.builder()
        val argType = storageType(argument.protocol, argument.type)
        val clearTextTemp = context.newTemporary("clearTextTemp")
        builder.addStatement(
            "val %L = %L",
            clearTextTemp,
            receiveReplicated(argType, senders.toList(), context),
        )
        // Check other receiving hosts for equivocation
        val peers = receivers.filter { it != context.host }
        if (peers.isNotEmpty()) {
            for (host in peers) builder.addStatement("%L", context.send(CodeBlock.of(clearTextTemp), host))
            builder.addStatement(
                "%L",
                receiveExpected(
                    CodeBlock.of(clearTextTemp),
                    senders.first(),
                    argType,
                    peers,
                    context,
                ),
            )
        }
        return Pair(builder.build(), CodeBlock.of(clearTextTemp))
    }

    private fun move(
        source: Protocol,
        target: Protocol,
        argument: Argument,
        builder: CodeBlock.Builder,
    ): CodeBlock {
        require(context.host in source.hosts || context.host in target.hosts)
        require(source is Cleartext)
        require(target is Cleartext)
        val receivingHosts = target.hosts - source.hosts
        return when (context.host) {
            in source.hosts -> {
                receivingHosts.forEach { builder.addStatement("%L", context.send(argument.value, it)) }
                argument.value
            }

            in receivingHosts -> {
                val (receiveCode, value) = receive(source.hosts, receivingHosts, argument)
                builder.add(receiveCode)
                value
            }

            else -> throw IllegalStateException()
        }
    }

    private fun checkPeerValues(
        peers: HostSetValue,
        value: CodeBlock,
        valueType: TypeName,
        builder: CodeBlock.Builder,
    ) {
        val receivingPeers = peers.filter { it != context.host }
        if (receivingPeers.isNotEmpty()) {
            for (host in receivingPeers) builder.addStatement("%L", context.send(value, host))
            builder.addStatement(
                "%L",
                receiveExpected(
                    value,
                    context.host,
                    valueType,
                    receivingPeers,
                    context,
                ),
            )
        }
    }

    private fun createCommitment(
        source: Protocol,
        target: Protocol,
        argument: Argument,
        builder: CodeBlock.Builder,
    ): CodeBlock {
        require(context.host in source.hosts + target.hosts)
        if (source !is Local) {
            throw UnsupportedCommunicationException(source, target, argument.sourceLocation)
        }
        require(target is CommitmentProtocol)
        if (target.cleartextHost != source.host) {
            throw UnsupportedCommunicationException(source, target, argument.sourceLocation)
        }

        val sendingHost = target.cleartextHost
        val receivingHosts = target.hashHosts
        return when (context.host) {
            sendingHost -> {
                val tempName1 = context.newTemporary("committed")
                val tempName2 = context.newTemporary("commitment")
                builder.addStatement(
                    "val %N = %T(%L)",
                    tempName1,
                    (Committed::class).asTypeName(),
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
                val argType = kotlinType(argument.type.shape, typeTranslator(argument.type.elementType.value))
                val tempName3 = context.newTemporary("commitment")
                builder.addStatement(
                    "val %N = %L",
                    tempName3,
                    context.receive((Commitment::class).asTypeName().parameterizedBy(argType), source.host),
                )
                checkPeerValues(HostSetValue(receivingHosts), CodeBlock.of("%L.hash", tempName3), BYTE_ARRAY, builder)
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
        require(source is CommitmentProtocol)
        if (target !is Cleartext) {
            throw UnsupportedCommunicationException(source, target, argument.sourceLocation)
        }
        require(context.host in source.hosts + target.hosts)

        val argType = kotlinType(argument.type.shape, typeTranslator(argument.type.elementType.value))
        val sendingHost = source.cleartextHost
        val commitmentReceivingHosts = target.hosts.filter { it in source.hashHosts }
        val cleartextReceivingHosts = target.hosts - commitmentReceivingHosts
        val omittedHosts = source.hashHosts - target.hosts
        val receivingHosts = target.hosts
        return when (context.host) {
            sendingHost -> {
                commitmentReceivingHosts.forEach {
                    builder.addStatement("%L", context.send(argument.value, it))
                }
                cleartextReceivingHosts.forEach {
                    builder.addStatement("%L", context.send(CodeBlock.of("%L.value", argument.value), it))
                }
                CodeBlock.of("%L.value", argument.value)
            }
            in commitmentReceivingHosts -> {
                val tempName1 = context.newTemporary("commitTemp")
                builder.addStatement(
                    "val %N = %L.%N(%L)",
                    tempName1,
                    argument.value,
                    "open",
                    context.receive((Committed::class).asTypeName().parameterizedBy(argType), source.cleartextHost),
                )
                checkPeerValues(receivingHosts, CodeBlock.of(tempName1), argType, builder)
                CodeBlock.of("%N", tempName1)
            }
            in cleartextReceivingHosts -> {
                val tempName1 = context.newTemporary("cleartextTemp")
                builder.addStatement(
                    "val %N = %L",
                    tempName1,
                    context.receive(argType, sendingHost),
                )
                checkPeerValues(receivingHosts, CodeBlock.of(tempName1), argType, builder)
                CodeBlock.of("%N", tempName1)
            }
            in omittedHosts -> {
                CodeBlock.of("")
            }
            else -> throw IllegalStateException()
        }
    }

    override fun import(
        protocol: Protocol,
        arguments: List<Argument>,
    ): Pair<CodeBlock, List<CodeBlock>> {
        require(protocol is Cleartext)
        val builder = CodeBlock.builder()
        val values = arguments.map { arg ->
            when (arg.protocol) {
                is Cleartext -> {
                    if (context.host in protocol.hosts + arg.protocol.hosts) {
                        move(arg.protocol, protocol, arg, builder)
                    } else {
                        CodeBlock.of("")
                    }
                }
                is CommitmentProtocol -> {
                    if (context.host in protocol.hosts + arg.protocol.hosts) {
                        openCommitment(arg.protocol, protocol, arg, builder)
                    } else {
                        CodeBlock.of("")
                    }
                }

                else -> throw UnsupportedCommunicationException(arg.protocol, protocol, arg.sourceLocation)
            }
        }
        return Pair(builder.build(), values)
    }

    override fun export(
        protocol: Protocol,
        arguments: List<Argument>,
    ): Pair<CodeBlock, List<CodeBlock>> {
        require(protocol is Cleartext)
        val builder = CodeBlock.builder()
        val values = arguments.map { arg ->
            when (arg.protocol) {
                is Cleartext -> {
                    if (context.host in protocol.hosts + arg.protocol.hosts) {
                        move(protocol, arg.protocol, arg, builder)
                    } else {
                        CodeBlock.of("")
                    }
                }
                is CommitmentProtocol -> {
                    if (context.host in protocol.hosts + arg.protocol.hosts) {
                        createCommitment(protocol, arg.protocol, arg, builder)
                    } else {
                        CodeBlock.of("")
                    }
                }

                else -> throw UnsupportedCommunicationException(protocol, arg.protocol, arg.sourceLocation)
            }
        }
        return Pair(builder.build(), values)
    }
}
