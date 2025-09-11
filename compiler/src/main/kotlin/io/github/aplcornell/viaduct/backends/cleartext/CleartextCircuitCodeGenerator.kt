package io.github.aplcornell.viaduct.backends.cleartext

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import io.github.aplcornell.viaduct.circuitcodegeneration.AbstractCodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.Argument
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.circuitcodegeneration.UnsupportedCommunicationException
import io.github.aplcornell.viaduct.circuitcodegeneration.receiveExpected
import io.github.aplcornell.viaduct.circuitcodegeneration.receiveReplicated
import io.github.aplcornell.viaduct.syntax.BinaryOperator
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.UnaryOperator
import io.github.aplcornell.viaduct.syntax.circuit.OperatorNode
import io.github.aplcornell.viaduct.syntax.operators.Maximum
import io.github.aplcornell.viaduct.syntax.operators.Minimum

class CleartextCircuitCodeGenerator(context: CodeGeneratorContext) : AbstractCodeGenerator(context) {
    override fun operatorApplication(
        protocol: Protocol,
        op: OperatorNode,
        arguments: List<CodeBlock>,
    ): CodeBlock =
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

    override fun import(
        protocol: Protocol,
        arguments: List<Argument>,
    ): Pair<CodeBlock, List<CodeBlock>> {
        require(protocol is Cleartext)
        val builder = CodeBlock.builder()
        val values =
            arguments.map { arg ->
                when (arg.protocol) {
                    is Cleartext -> {
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

    override fun export(
        protocol: Protocol,
        arguments: List<Argument>,
    ): Pair<CodeBlock, List<CodeBlock>> {
        require(protocol is Cleartext)
        val builder = CodeBlock.builder()
        val values =
            arguments.map { arg ->
                when (arg.protocol) {
                    is Cleartext -> {
                        if (context.host in protocol.hosts + arg.protocol.hosts) {
                            move(protocol, arg.protocol, arg, builder)
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
