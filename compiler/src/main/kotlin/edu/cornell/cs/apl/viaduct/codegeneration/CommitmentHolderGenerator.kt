package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.runtime.commitment.Commitment
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.backends.commitment.Commitment as CommitmentProtocol

class CommitmentHolderGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    override fun exp(expr: ExpressionNode, protocol: Protocol): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(
                "%M(%L).%M()",
                MemberName(Committed.Companion::class.asClassName(), "fake"),
                expr.value,
                MemberName(Committed.Companion::class.asClassName(), "commitment")
            )

            is ReadNode -> CodeBlock.of(
                "%L",
                context.kotlinName(expr.temporary.value, protocol)
            )

            is DowngradeNode -> exp(expr.expression, protocol)

            is QueryNode ->
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

            is OperatorApplicationNode ->
                throw CodeGenerationError("Commitment: cannot perform operations on committed values")

            is InputNode ->
                throw CodeGenerationError("Commitment: cannot perform I/O in non-local protocol")
        }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        CodeBlock.of(
            "var %N = %L",
            context.kotlinName(stmt.temporary.value, protocol),
            exp(stmt.value, protocol)
        )

    override fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock =
        declarationHelper(
            context.kotlinName(stmt.name.value),
            stmt.className,
            stmt.arguments,
            CodeBlock.of(
                "%M(%L).%M()",
                MemberName(Committed.Companion::class.asClassName(), "fake"),
                exp(stmt.typeArguments[0].value.defaultValue),
                MemberName(Committed.Companion::class.asClassName(), "commitment")
            ),
            protocol
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

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        throw CodeGenerationError("Commitment: cannot perform I/O in non-local protocol")

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        throw CodeGenerationError("Commitment: cannot use committed value as a guard")

    override fun send(
        sendingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()

        // here, the interpreter checks for the available protocols, is this necessary here?
        val relevantEvents: Set<CommunicationEvent> =
            events.getProjectionSends(
                ProtocolProjection(sendProtocol, sendingHost),
                CommitmentProtocol.OPEN_COMMITMENT_OUTPUT
            )

        for (event in relevantEvents) {
            if (event.send.host != event.recv.host) {
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
        val clearTextTemp = context.newTemporary("clearTextTemp")
        if (sendProtocol != receiveProtocol) {
            when {
                events.any { event -> event.recv.id == CommitmentProtocol.CLEARTEXT_INPUT } -> {
                    val relevantEvents =
                        events.getHostReceives(
                            projection.host,
                            CommitmentProtocol.CLEARTEXT_INPUT
                        )

                    receiveBuilder.add(
                        receiveHelper(
                            sender,
                            sendProtocol,
                            relevantEvents,
                            context,
                            clearTextTemp,
                            "Commitment Holder Cleartext : received different values"
                        )
                    )

                    receiveBuilder.addStatement(
                        "val %N = %N",
                        context.kotlinName(sender.temporary.value, receiveProtocol),
                        clearTextTemp
                    )
                }

                else -> { // create commitment
                    if (receivingHost !in sendProtocol.hosts) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            context.kotlinName(sender.temporary.value, receiveProtocol),
                            context.receive(
                                Commitment::class.asClassName().parameterizedBy(
                                    typeTranslator(context.typeAnalysis.type(sender))
                                ),
                                events.first().send.host
                            )
                        )
                    }
                }
            }
        }
        return receiveBuilder.build()
    }
}
