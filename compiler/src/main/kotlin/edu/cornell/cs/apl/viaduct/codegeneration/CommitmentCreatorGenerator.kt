package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.RuntimeError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.runtime.commitment.Committed
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType

class CommitmentCreatorGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    private val runtimeErrorClass = RuntimeError::class
    private val committedClassName = Committed::class

    // this function should take in a protocol and reference the protocol arg in the body
    override fun exp(expr: ExpressionNode, protocol: Protocol): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(
                "%T(%L)",
                committedClassName,
                expr.value
            )

            is ReadNode ->
                CodeBlock.of(
                    "%N",
                    context.kotlinName(
                        expr.temporary.value,
                        protocol
                    )
                )

            is DowngradeNode -> exp(expr.expression, protocol)

            is QueryNode -> {
                when (context.typeAnalysis.type(context.nameAnalysis.declaration(expr))) {
                    is VectorType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(
                                "%N[%L]",
                                context.kotlinName(expr.variable.value),
                                exp(expr.arguments.first(), protocol)
                            )
                            else -> throw CodeGenerationError("unknown vector query", expr)
                        }
                    }

                    is ImmutableCellType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(context.kotlinName(expr.variable.value))
                            else -> throw CodeGenerationError("unknown query", expr)
                        }
                    }

                    is MutableCellType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(context.kotlinName(expr.variable.value))
                            else -> throw CodeGenerationError("unknown query", expr)
                        }
                    }

                    else -> throw CodeGenerationError("unknown AST object", expr)
                }
            }

            is OperatorApplicationNode ->
                throw CodeGenerationError("Commitment: cannot perform operations on committed values")

            is InputNode ->
                throw CodeGenerationError("Commitment: cannot perform I/O in non-local protocol")

            is ReceiveNode ->
                throw IllegalInternalCommunicationError(expr)
        }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        CodeBlock.of(
            "val %N = %L",
            context.kotlinName(stmt.temporary.value, protocol),
            exp(stmt.value, protocol)
        )

    override fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock =
        declarationHelper(
            context.kotlinName(stmt.name.value),
            stmt.className,
            stmt.arguments,
            CodeBlock.of(
                "fake(%L)",
                exp(stmt.typeArguments[0].value.defaultValue)
            )
        )

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (context.typeAnalysis.type(context.nameAnalysis.declaration(stmt))) {
            is VectorType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N[%L] = %L",
                            context.kotlinName(stmt.variable.value),
                            exp(stmt.arguments[0], protocol),
                            exp(stmt.arguments[1], protocol)
                        )
                    else -> throw CodeGenerationError("Commitment: cannot modify commitments")
                }
            is MutableCellType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N = %L",
                            context.kotlinName(stmt.variable.value),
                            exp(stmt.arguments[0], protocol)
                        )
                    else -> throw CodeGenerationError("Commitment: cannot modify commitments")
                }
            else -> throw CodeGenerationError("Commitment: unknown object to update")
        }

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock {
        throw ViaductInterpreterError("Commitment: cannot perform I/O in non-local protocol")
    }

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock = exp(expr, protocol)

    override fun send(
        sendingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()
        if (sendProtocol != receiveProtocol) {
            val relevantEvents: Set<CommunicationEvent> =
                events.getProjectionSends(ProtocolProjection(sendProtocol, sendingHost))

            for (event in relevantEvents) {

                // send temporary containing hash value
                sendBuilder.addStatement(
                    "%L",
                    context.send(
                        CodeBlock.of("%L", context.kotlinName(sender.temporary.value, sendProtocol)),
                        event.recv.host
                    )
                )
            }
        }
        return sendBuilder.build()
    }

    override fun receive(
        receivingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val receiveBuilder = CodeBlock.builder()
        val projection = ProtocolProjection(receiveProtocol, receivingHost)
        val hashHosts: Set<Host> = (projection.protocol as Commitment).hashHosts
        var clearTextTemp = context.newTemporary("clearTextTemp")
        val committedTemp = context.newTemporary("committed")
        val commitTemp = context.newTemporary("commitment")
        if (sendProtocol != receiveProtocol) {
            when {
                events.any { event -> event.recv.id == Commitment.CLEARTEXT_INPUT } -> {
                    var relevantEvents =
                        events.getHostReceives(
                            projection.host,
                            Commitment.CLEARTEXT_INPUT
                        ).filter { event -> event.send.host != context.host }

                    if (relevantEvents.isNotEmpty()) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            clearTextTemp,
                            context.receive(
                                typeTranslator(context.typeAnalysis.type(sender)),
                                relevantEvents.first().send.host
                            )
                        )
                        relevantEvents = relevantEvents.minusElement(relevantEvents.first())
                    }

                    // receive from the rest of the hosts and compare against clearTextValue
                    for (event in relevantEvents) {

                        // check to make sure that you got the same data from all hosts
                        receiveBuilder.beginControlFlow(
                            "if (%N != %L)",
                            clearTextTemp,
                            context.receive(
                                typeTranslator(context.typeAnalysis.type(sender)),
                                event.send.host
                            )
                        )
                        receiveBuilder.addStatement(
                            "throw %T(%S)",
                            runtimeErrorClass,
                            "Commitment Cleartext : received different values"
                        )
                        receiveBuilder.endControlFlow()
                    }
                }

                else -> {
                    var cleartextInputEvents =
                        events.getProjectionReceives(
                            projection,
                            Commitment.INPUT
                        ).filter { event -> event.send.host != context.host }

                    if (cleartextInputEvents.isNotEmpty()) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            clearTextTemp,
                            context.receive(
                                typeTranslator(context.typeAnalysis.type(sender)),
                                cleartextInputEvents.first().send.host
                            )
                        )
                        cleartextInputEvents = cleartextInputEvents.minusElement(cleartextInputEvents.first())
                    } else {
                        clearTextTemp = context.kotlinName(sender.temporary.value, sendProtocol)
                    }

                    // receive from the rest of the hosts and compare against clearTextValue
                    for (event in cleartextInputEvents) {

                        // check to make sure that you got the same data from all hosts
                        receiveBuilder.beginControlFlow(
                            "if (%N != %L)",
                            clearTextTemp,
                            context.receive(
                                typeTranslator(context.typeAnalysis.type(sender)),
                                event.send.host
                            )
                        )
                        receiveBuilder.addStatement(
                            "throw %T(%S)",
                            runtimeErrorClass,
                            "Commitment Cleartext : received different values"
                        )
                        receiveBuilder.endControlFlow()
                    }

                    receiveBuilder.addStatement(
                        "val %N = %T(%N)",
                        committedTemp,
                        committedClassName.asClassName().parameterizedBy(
                            typeTranslator(context.typeAnalysis.type(sender))
                        ),
                        clearTextTemp
                    )

                    // create commitment
                    receiveBuilder.addStatement(
                        "val %N = %N.%M()",
                        commitTemp,
                        committedTemp,
                        MemberName(Committed.Companion::class.asClassName(), "commitment")
                    )

                    for (hashHost in hashHosts) {
                        receiveBuilder.addStatement(
                            "%L",
                            context.send(
                                CodeBlock.of(
                                    "%L",
                                    commitTemp
                                ),
                                hashHost
                            )
                        )
                    }

//                    receiveBuilder.addStatement(
//                        "val %N = %N",
//                        context.kotlinName(sender.temporary.value, sendProtocol),
//                        committedTemp
//                    )
                }
            }
        }
        return receiveBuilder.build()
    }
}
