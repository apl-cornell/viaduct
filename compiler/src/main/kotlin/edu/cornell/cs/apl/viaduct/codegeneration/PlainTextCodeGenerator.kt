package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asClassName
import edu.cornell.cs.apl.prettyprinting.joined
import edu.cornell.cs.apl.viaduct.errors.RuntimeError
import edu.cornell.cs.apl.viaduct.protocols.Plaintext
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode

class PlainTextCodeGenerator(
    program: ProgramNode,
    override val availableProtocols: Set<Protocol>
) : AbstractCodeGenerator(program) {
    val runtimeErrorClassName = RuntimeError::class.asClassName()

    private fun exp(expr: ExpressionNode): Any {
        return when (expr) {
            is LiteralNode -> expr.value

            // TODO - this is generating .get() calls, how to fix this?
            is ReadNode -> expr.temporary.value.name

            is OperatorApplicationNode -> expr.operator.asDocument(expr.arguments).print()

            is QueryNode -> expr.variable.value.name + "." + expr.query.value.name + "(" + expr.arguments.joined().print() + ")"

            is DowngradeNode -> exp(expr)

            is InputNode -> {
                val type = expr.type::class.toString()
                "runtime.input($type)"
            }

            is ReceiveNode -> TODO()
        }
    }

    override fun Let(protocol: Protocol, stmt: LetNode): CodeBlock {
        return CodeBlock.of(
            "val %L = %L",
            stmt.temporary.value.name,
            exp(stmt.value)
        )
    }

    fun DeclarationHelper(
        name: String,
        className: ClassNameNode,
        arguments: Arguments<AtomicExpressionNode>
    ): CodeBlock {
        return when (className.value) {
            ImmutableCell -> CodeBlock.of(
                "val %L = %L",
                name,
                arguments.joined().print()
            )

            // TODO - change this (difference between viaduct, kotlin semantics)
            MutableCell -> CodeBlock.of(
                "var %L = %L",
                name,
                arguments.joined().print()
            )

            Vector -> CodeBlock.of(
                "var %L = Array(%L)",
                name,
                arguments.joined().print()
            )

            else -> CodeBlock.of(
                "var %L = %T(%L)",
                name,
                className.value.name::class.asClassName(),
                arguments.joined().print()
            )
        }
    }

    override fun Declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock {
        return DeclarationHelper(stmt.name.value.name, stmt.className, stmt.arguments)
    }

    override fun Update(protocol: Protocol, stmt: UpdateNode): CodeBlock {
        return CodeBlock.of(
            "%L.%L(%L)",
            stmt.variable.value.name,
            stmt.update.value.name,
            stmt.arguments.joined().print()
        )
    }

    override fun OutParameterInitialization(
        protocol: Protocol,
        stmt: OutParameterInitializationNode
    ): CodeBlock {
        return when (val initializer = stmt.initializer) {
            is OutParameterConstructorInitializerNode -> {
                CodeBlock.builder()
                    .add(
                        // declare object
                        DeclarationHelper(
                            "outTemp",
                            initializer.className,
                            initializer.arguments
                        )
                    )
                    .add(
                        // fill box named [stmt.name.value.name] with constructed object
                        CodeBlock.of(
                            "%L.set(%L)",
                            stmt.name.value.name,
                            "outTemp"
                        )
                    )
                    .build()
            }

            // fill box named [stmt.name.value.name] with [initializer.expression]
            is OutParameterExpressionInitializerNode -> {
                CodeBlock.of("%L.set(%L)",
                    stmt.name.value.name,
                    exp(initializer.expression)
                )
            }
        }
    }

    override fun Output(protocol: Protocol, stmt: OutputNode): CodeBlock {
        return CodeBlock.of(
            "runtime.output(%L)",
            exp(stmt.message)
        )
    }

    override fun Guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock {
        return CodeBlock.of(exp(expr) as String)
    }

    override fun Send(
        sendingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()
        if (sendProtocol != recvProtocol) {
            val relevantEvents: Set<CommunicationEvent> =
                events.getProjectionSends(ProtocolProjection(sendProtocol, sendingHost))
            for (event in relevantEvents) {
                sendBuilder.addStatement(
                    "runtime.send(%L, runtime.getHost(%L))",
                    sender.temporary.value,
                    event.recv.host.name
                )
            }
        }
        return sendBuilder.build()
    }

    // note - this implementation does not support commitments
    override fun Recieve(
        receivingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val receiveBuilder = CodeBlock.builder()
        if (sendProtocol != recvProtocol) {
            val projection = ProtocolProjection(recvProtocol, receivingHost)
            val cleartextInputs = events.getProjectionReceives(projection, Plaintext.INPUT)

            // initialize cleartext receive value
            receiveBuilder.addStatement("val cleartextValue = null")
            receiveBuilder.beginControlFlow(
                "for (i in 1..%L)",
                cleartextInputs.size
            )

            // receive from all hosts who send data for [sender]
            for (event in cleartextInputs) {
                receiveBuilder.addStatement(
                    "val receivedValue = runtime.receive(runtime.getHost(%L))",
                    event.send.host.name
                )
                receiveBuilder.beginControlFlow("if(cleartextValue == null)")
                receiveBuilder.addStatement("cleartextValue = receivedValue")
                receiveBuilder.endControlFlow()

                // check to make sure that you got the same data from all hosts
                receiveBuilder.beginControlFlow("else if(cleartextValue != receivedValue)")
                receiveBuilder.addStatement(
                    "throw %T(%S)",
                    runtimeErrorClassName,
                    "Plaintext : received different values"
                )
                receiveBuilder.endControlFlow()
            }
            receiveBuilder.endControlFlow()

            // check if value you received is null
            receiveBuilder.beginControlFlow("if(cleartextValue == null")
            receiveBuilder.addStatement(
                "throw %T(%S)",
                runtimeErrorClassName,
                "Plaintext : received null value"
            )
            receiveBuilder.endControlFlow()

            // calculate set of hosts with whom [receivingHost] needs to check for equivocation
            val hostsToCheckWith: Set<Host> =
                events
                    .filter { event ->
                        // remove events where receiving host is not receiving plaintext data
                        event.recv.id == Plaintext.INPUT &&

                            // remove events where a host is sending data to themselves
                            event.send.host != event.recv.host &&

                            // remove events where [receivingHost] is the sender of the data
                            event.send.host != receivingHost
                    }

                    // of events matching above criteria, get set of data receivers
                    .map { event -> event.recv.host }

                    // remove [receivingHost] from the set of hosts with whom [receivingHost] needs to
                    // check for equivocation
                    .filter { host -> host != receivingHost }
                    .toSet()

            for (host in hostsToCheckWith) {
                receiveBuilder.addStatement(
                    "runtime.send(cleartextValue, runtime.getHost(%L))",
                    host.name
                )
            }

            for (host in hostsToCheckWith) {
                receiveBuilder.addStatement(
                    "var recvValue = runtime.receive(runtime.getHost(%L))",
                    host.name
                )
                receiveBuilder.beginControlFlow("if(recvValue != cleartextValue)")
                receiveBuilder.addStatement(
                    "throw %T(%S)",
                    runtimeErrorClassName,
                    "equivocation error between hosts: " + receivingHost.asDocument.print() + ", " +
                        host.asDocument.print()
                )
                receiveBuilder.endControlFlow()
            }
        }
        return receiveBuilder.build()
    }
}
