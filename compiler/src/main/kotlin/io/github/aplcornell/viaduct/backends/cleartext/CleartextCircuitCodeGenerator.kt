package io.github.aplcornell.viaduct.backends.cleartext

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import io.github.aplcornell.viaduct.circuitcodegeneration.AbstractCodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.Argument
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.circuitcodegeneration.UnsupportedCommunicationException
import io.github.aplcornell.viaduct.circuitcodegeneration.UnsupportedOperatorException
import io.github.aplcornell.viaduct.circuitcodegeneration.receiveExpected
import io.github.aplcornell.viaduct.circuitcodegeneration.receiveReplicated
import io.github.aplcornell.viaduct.syntax.BinaryOperator
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.UnaryOperator
import io.github.aplcornell.viaduct.syntax.circuit.OperatorNode
import io.github.aplcornell.viaduct.syntax.operators.Maximum
import io.github.aplcornell.viaduct.syntax.operators.Minimum

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

            else -> throw UnsupportedOperatorException(protocol, op)
        }

    private fun send(
        receiver: Protocol,
        argument: Argument,
    ): CodeBlock {
        val builder = CodeBlock.builder()
        receiver.hosts.forEach {
            if (it != context.host) builder.addStatement("%L", context.send(argument.value, it))
        }
        return builder.build()
    }

    private fun receive(
        receiver: Protocol,
        argument: Argument,
    ): Pair<CodeBlock, CodeBlock> {
        val builder = CodeBlock.builder()
        val argType = storageType(argument.protocol, argument.type)
        val clearTextTemp = context.newTemporary("clearTextTemp")
        builder.addStatement(
            "val %L = %L",
            clearTextTemp,
            receiveReplicated(argType, argument.protocol.hosts.toList(), context),
        )
        // Check other receiving hosts for equivocation
        val peers = receiver.hosts.filter { it != context.host }
        for (host in peers) builder.addStatement("%L", context.send(CodeBlock.of(clearTextTemp), host))
        builder.add(
            receiveExpected(
                CodeBlock.of(clearTextTemp),
                argument.protocol.hosts.first(),
                argType,
                peers,
                context,
            ),
        )
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
        return when (context.host) {
            in source.hosts -> {
                builder.add(send(target, argument))
                argument.value
            }

            in target.hosts -> {
                val (receiveCode, value) = receive(target, argument)
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
        val values = arguments.map { arg ->
            when (arg.protocol) {
                is Cleartext -> {
                    move(arg.protocol, protocol, arg, builder)
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
                    move(protocol, arg.protocol, arg, builder)
                }

                else -> throw UnsupportedCommunicationException(protocol, arg.protocol, arg.sourceLocation)
            }
        }
        return Pair(builder.build(), values)
    }
}
